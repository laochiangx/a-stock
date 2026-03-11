package com.example.stock.service.impl;

import com.example.stock.dao.TdxLbttDayDao;
import com.example.stock.dao.TdxLbttItemDao;
import com.example.stock.entity.TdxLbttDay;
import com.example.stock.entity.TdxLbttItem;
import com.example.stock.service.TdxLbttService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TdxLbttServiceImpl implements TdxLbttService {

    private static final Logger log = LoggerFactory.getLogger(TdxLbttServiceImpl.class);

    private static final String BASE_URL = "http://hot.icfqs.com:7615";
    private static final String ENTRY_LBTT = "CWServ.cfg_fx_lbtt";
    private static final String ENTRY_PBSDSTAT = "HQServ.PBSdstat";

    private static final String REFERER = "http://hot.icfqs.com:7615/site/tdx-pc-hqpage/page-lbtt.html?color=0&bkcolor=141212";
    private static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");

    @Autowired
    private TdxLbttDayDao tdxLbttDayDao;

    @Autowired
    private TdxLbttItemDao tdxLbttItemDao;

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public TdxLbttServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> sync(String startDate, String endDate, Boolean force) {
        Map<String, Object> result = new HashMap<>();
        boolean f = force != null && force;

        LocalDate end = parseDate(endDate);
        LocalDate start = parseDate(startDate);
        if (start.isAfter(end)) {
            LocalDate t = start;
            start = end;
            end = t;
        }

        result.put("start", start.toString());
        result.put("end", end.toString());

        try {
            String startStr = start.format(DateTimeFormatter.BASIC_ISO_DATE);
            String endStr = end.format(DateTimeFormatter.BASIC_ISO_DATE);

            JsonNode lbtt1 = postEntry(ENTRY_LBTT, buildParamsBody("1", startStr, endStr));
            JsonNode lbtt2 = postEntry(ENTRY_LBTT, buildParamsBody("2", startStr, endStr));
            JsonNode pbsdstat = postEntry(ENTRY_PBSDSTAT, "{\"Head\":{\"Target\":0},\"Type\":\"4\"}");

            List<TdxLbttItem> items = parseLbttItems(lbtt1);
            Map<LocalDate, TdxLbttDay> dayMap = parseLbttDays(lbtt2);

            LocalDate pbsdDate = end;
            if (dayMap.containsKey(end)) {
                pbsdDate = end;
            } else if (!dayMap.isEmpty()) {
                pbsdDate = dayMap.keySet().stream().max(LocalDate::compareTo).orElse(end);
            }

            if (pbsdstat != null && dayMap.containsKey(pbsdDate)) {
                try {
                    TdxLbttDay day = dayMap.get(pbsdDate);
                    day.setPbsdstatJson(mapper.writeValueAsString(pbsdstat));
                    applyPbsdstatToDay(pbsdstat, day);
                } catch (Exception ignore) {
                }
            }

            Map<LocalDate, List<TdxLbttItem>> itemMap = new HashMap<>();
            for (TdxLbttItem it : items) {
                if (it.getDataDate() == null) continue;
                itemMap.computeIfAbsent(it.getDataDate(), k -> new ArrayList<>()).add(it);
            }

            Set<LocalDate> allDates = new LinkedHashSet<>();
            allDates.addAll(dayMap.keySet());
            allDates.addAll(itemMap.keySet());

            int dayUpsert = 0;
            int itemUpsert = 0;
            int skipped = 0;

            for (LocalDate d : allDates) {
                if (d == null) continue;
                int dayCnt = tdxLbttDayDao.countByDate(d);
                int itemCnt = tdxLbttItemDao.countByDate(d);
                if (!f && dayCnt > 0 && itemCnt > 0) {
                    skipped++;
                    continue;
                }

                tdxLbttItemDao.physicalDeleteByDate(d);
                tdxLbttDayDao.physicalDeleteByDate(d);

                TdxLbttDay day = dayMap.get(d);
                if (day != null) {
                    tdxLbttDayDao.insert(day);
                    dayUpsert++;
                }

                List<TdxLbttItem> list = itemMap.get(d);
                if (list != null && !list.isEmpty()) {
                    for (TdxLbttItem it : list) {
                        tdxLbttItemDao.insert(it);
                        itemUpsert++;
                    }
                }
            }

            result.put("success", true);
            result.put("daysUpsert", dayUpsert);
            result.put("itemsUpsert", itemUpsert);
            result.put("skipped", skipped);
            return result;
        } catch (Exception e) {
            log.error("tdx lbtt sync failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> query(String startDate, String endDate, Integer days) {
        Map<String, Object> result = new HashMap<>();

        LocalDate end = parseDate(endDate);
        LocalDate start;
        if (days != null && days > 0) {
            start = end.minusDays(days - 1L);
        } else {
            start = parseDate(startDate);
        }

        if (start.isAfter(end)) {
            LocalDate t = start;
            start = end;
            end = t;
        }

        try {
            List<TdxLbttDay> dayList = tdxLbttDayDao.findByRange(start, end);
            List<TdxLbttItem> itemList = tdxLbttItemDao.findByRange(start, end);

            result.put("success", true);
            result.put("start", start.toString());
            result.put("end", end.toString());
            result.put("days", dayList);
            result.put("items", itemList);
            result.put("dayCount", dayList == null ? 0 : dayList.size());
            result.put("itemCount", itemList == null ? 0 : itemList.size());
            return result;
        } catch (Exception e) {
            log.error("tdx lbtt query failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("start", start.toString());
            result.put("end", end.toString());
            return result;
        }
    }

    private String buildParamsBody(String type, String startYmd, String endYmd) {
        return "{\"Params\":[\"" + type + "\",\"" + startYmd + "\",\"" + endYmd + "\"]}";
    }

    private JsonNode postEntry(String entry, String bodyJson) throws Exception {
        String url = BASE_URL + "/TQLEX?Entry=" + entry + "&RI=";
        RequestBody body = RequestBody.create(bodyJson, TEXT);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("accept", "*/*")
                .addHeader("content-type", "text/plain;charset=UTF-8")
                .addHeader("origin", BASE_URL)
                .addHeader("referer", REFERER)
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("cookie", "LST=00")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code() + " " + response.message());
            }
            String text = response.body().string();
            if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            return mapper.readTree(text);
        }
    }

    private Map<LocalDate, TdxLbttDay> parseLbttDays(JsonNode root) {
        Map<LocalDate, TdxLbttDay> map = new HashMap<>();
        if (root == null || root.isNull()) return map;
        if (root.path("ErrorCode").asInt(-1) != 0) return map;

        JsonNode rs0 = root.path("ResultSets").isArray() && root.path("ResultSets").size() > 0 ? root.path("ResultSets").get(0) : null;
        if (rs0 == null) return map;

        Map<String, Integer> idx = buildIndex(rs0.path("ColName"));
        JsonNode content = rs0.path("Content");
        if (!content.isArray()) return map;

        for (JsonNode row : content) {
            if (row == null || !row.isArray()) continue;
            String dateStr = asText(row, idx, "t001");
            if (dateStr == null || dateStr.trim().isEmpty()) continue;

            LocalDate d = parseDate(dateStr.trim());
            TdxLbttDay day = new TdxLbttDay();
            day.setDataDate(d);
            day.setRisingCount(asInt(row, idx, "N002"));
            day.setFallingCount(asInt(row, idx, "N003"));
            day.setZtAllCount(asInt(row, idx, "N004"));
            day.setZtCount(asInt(row, idx, "N005"));
            day.setDtCount(asInt(row, idx, "N006"));
            day.setTotalAmount(asBigDecimal(row, idx, "N007"));
            day.setLastTotalAmount(asBigDecimal(row, idx, "N008"));
            day.setHot1Name(asText(row, idx, "N009"));
            day.setHot1Count(asInt(row, idx, "N010"));
            day.setHot2Name(asText(row, idx, "N011"));
            day.setHot2Count(asInt(row, idx, "N012"));
            day.setHot3Name(asText(row, idx, "N013"));
            day.setHot3Count(asInt(row, idx, "N014"));

            try {
                ObjectNode raw = mapper.createObjectNode();
                for (Map.Entry<String, Integer> e : idx.entrySet()) {
                    JsonNode v = row.get(e.getValue());
                    raw.set(e.getKey(), v);
                }
                day.setRawJson(mapper.writeValueAsString(raw));
            } catch (Exception ignore) {
            }

            map.put(d, day);
        }

        return map;
    }

    private List<TdxLbttItem> parseLbttItems(JsonNode root) {
        List<TdxLbttItem> list = new ArrayList<>();
        if (root == null || root.isNull()) return list;
        if (root.path("ErrorCode").asInt(-1) != 0) return list;

        JsonNode rs0 = root.path("ResultSets").isArray() && root.path("ResultSets").size() > 0 ? root.path("ResultSets").get(0) : null;
        if (rs0 == null) return list;

        Map<String, Integer> idx = buildIndex(rs0.path("ColName"));
        JsonNode content = rs0.path("Content");
        if (!content.isArray()) return list;

        for (JsonNode row : content) {
            if (row == null || !row.isArray()) continue;
            String dateStr = asText(row, idx, "rqex");
            if (dateStr == null || dateStr.trim().isEmpty()) continue;
            LocalDate d = parseDate(dateStr.trim());

            TdxLbttItem it = new TdxLbttItem();
            it.setDataDate(d);
            it.setMaxLevel(asInt(row, idx, "zglb"));
            it.setLevel(asInt(row, idx, "lbts"));
            it.setStockCode(asText(row, idx, "ZQDM"));
            it.setMarket(asText(row, idx, "SC"));
            it.setStockName(asText(row, idx, "ZQJC"));
            it.setReason(asText(row, idx, "ztyy"));
            it.setReason2(asText(row, idx, "ztyy2"));
            it.setZtTime(asText(row, idx, "ztsj"));
            it.setSealAmount(asBigDecimal(row, idx, "fde"));
            it.setOpenTimes(asInt(row, idx, "kbcs"));
            it.setIndustry(asText(row, idx, "sshy"));
            it.setZtState(asIntNullable(row, idx, "ztlb"));
            it.setPromoteRate(asBigDecimal(row, idx, "cgl"));

            try {
                ObjectNode raw = mapper.createObjectNode();
                for (Map.Entry<String, Integer> e : idx.entrySet()) {
                    JsonNode v = row.get(e.getValue());
                    raw.set(e.getKey(), v);
                }
                it.setRawJson(mapper.writeValueAsString(raw));
            } catch (Exception ignore) {
            }

            list.add(it);
        }

        return list;
    }

    private void applyPbsdstatToDay(JsonNode pbsdstat, TdxLbttDay day) {
        if (pbsdstat == null || day == null) return;
        JsonNode arr = pbsdstat.path("scglobaldata");
        if (!arr.isArray() || arr.size() <= 0) return;
        JsonNode row = arr.get(0);
        if (row == null || row.isNull()) return;
        int ztnum = row.path("ztnum").asInt(0);
        int dtnum = row.path("dtnum").asInt(0);
        if (ztnum > 0) {
            day.setZtCount(ztnum);
        }
        if (dtnum > 0) {
            day.setDtCount(dtnum);
        }
    }

    private Map<String, Integer> buildIndex(JsonNode colNames) {
        Map<String, Integer> idx = new HashMap<>();
        if (colNames == null || !colNames.isArray()) return idx;
        for (int i = 0; i < colNames.size(); i++) {
            String name = colNames.get(i).asText("");
            if (name != null && !name.trim().isEmpty()) {
                idx.put(name.trim(), i);
            }
        }
        return idx;
    }

    private String asText(JsonNode row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) return null;
        JsonNode v = row.get(i);
        if (v == null || v.isNull()) return null;
        String s = v.asText(null);
        return s == null ? null : s.trim();
    }

    private Integer asInt(JsonNode row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) return 0;
        JsonNode v = row.get(i);
        if (v == null || v.isNull()) return 0;
        if (v.isInt() || v.isLong()) return v.asInt();
        try {
            String s = v.asText("0").trim();
            if (s.isEmpty()) return 0;
            if (s.contains(".")) return (int) Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private Integer asIntNullable(JsonNode row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) return null;
        JsonNode v = row.get(i);
        if (v == null || v.isNull()) return null;
        if (v.isInt() || v.isLong()) return v.asInt();
        try {
            String s = v.asText("").trim();
            if (s.isEmpty()) return null;
            if (s.contains(".")) return (int) Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private BigDecimal asBigDecimal(JsonNode row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) return null;
        JsonNode v = row.get(i);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) {
            try {
                return v.decimalValue();
            } catch (Exception ignore) {
                return BigDecimal.valueOf(v.asDouble());
            }
        }
        String s = v.asText("").trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        String d = date.trim();
        try {
            if (d.matches("\\d{8}")) {
                return LocalDate.parse(d, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception ignore) {
        }
        return LocalDate.now();
    }
}
