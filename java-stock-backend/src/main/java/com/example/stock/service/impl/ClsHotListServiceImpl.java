package com.example.stock.service.impl;

import com.example.stock.dao.ClsHotListDayDao;
import com.example.stock.entity.ClsHotListDay;
import com.example.stock.service.ClsHotListService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClsHotListServiceImpl implements ClsHotListService {

    private static final Logger log = LoggerFactory.getLogger(ClsHotListServiceImpl.class);

    private static final String CLS_APP = "cailianpress";
    private static final String CLS_SV = "835";
    private static final String CLS_OS = "android";

    private static final String API3HOST = "https://api3.cls.cn";
    private static final String XQUOTE = "https://x-quote.cls.cn";

    private static final String HOT_STOCK_URL = API3HOST + "/v1/hot_stock";
    private static final String HOT_LIST_URL = API3HOST + "/v1/hot_list";
    private static final String UP_REASON_URL = XQUOTE + "/v2/quote/a/stock/up_reason";

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ClsHotListDayDao clsHotListDayDao;

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public ClsHotListServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> syncDayHotList(String date, Boolean force, String listType) {
        Map<String, Object> result = new HashMap<>();
        String lt = (listType == null || listType.trim().isEmpty()) ? "stock" : listType.trim();
        LocalDate queryDate = parseDate(date);
        boolean f = force != null && force;

        try {
            if (f) {
                clsHotListDayDao.physicalDelete(queryDate, lt);
            }

            int existing = clsHotListDayDao.countByDate(queryDate, lt);
            if (existing > 0 && !f) {
                result.put("success", true);
                result.put("date", queryDate.toString());
                result.put("listType", lt);
                result.put("count", existing);
                result.put("message", "exists");
                return result;
            }

            List<ClsHotListDay> fetched = fetchDayHotListFromApi(queryDate, lt);

            clsHotListDayDao.physicalDelete(queryDate, lt);
            for (ClsHotListDay row : fetched) {
                clsHotListDayDao.insert(row);
            }

            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("listType", lt);
            result.put("count", fetched.size());
        } catch (Exception e) {
            log.error("syncDayHotList failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("listType", lt);
        }

        return result;
    }

    @Override
    public Map<String, Object> listDayHotList(String date, String listType) {
        Map<String, Object> result = new HashMap<>();
        String lt = (listType == null || listType.trim().isEmpty()) ? "stock" : listType.trim();
        LocalDate queryDate = parseDate(date);
        try {
            List<ClsHotListDay> list = clsHotListDayDao.findByDate(queryDate, lt);
            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("listType", lt);
            result.put("count", list.size());
            result.put("data", list);
        } catch (Exception e) {
            log.error("listDayHotList failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("listType", lt);
        }
        return result;
    }

    private List<ClsHotListDay> fetchDayHotListFromApi(LocalDate queryDate, String listType) throws Exception {
        try {
            if ("info".equalsIgnoreCase(listType) || "hot_list".equalsIgnoreCase(listType)) {
                return fetchFromHotList(queryDate, listType);
            }
            return fetchFromHotStock(queryDate, listType);
        } catch (Exception e) {
            log.warn("CLS fetch failed, will return empty. msg={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ClsHotListDay> fetchFromHotStock(LocalDate queryDate, String listType) throws Exception {
        String params = "app=" + CLS_APP + "&os=" + CLS_OS + "&sv=" + CLS_SV;
        String sign = clsSign(params);

        HttpUrl url = HttpUrl.parse(HOT_STOCK_URL).newBuilder()
                .query(params + "&sign=" + sign)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "okhttp/4.9.3")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code() + " " + response.message());
            }
            String body = response.body().string();
            JsonNode root = mapper.readTree(body);
            if (root.path("errno").asInt(-1) != 0) {
                throw new RuntimeException("errno=" + root.path("errno").asText() + ", msg=" + root.path("msg").asText(""));
            }
            JsonNode list = root.path("data");
            if (list == null || list.isNull() || !list.isArray()) {
                return new ArrayList<>();
            }

            List<String> secuCodes = new ArrayList<>();
            for (JsonNode item : list) {
                String stockId = asText(item.path("stock"), "StockID");
                if (stockId != null && !stockId.trim().isEmpty()) {
                    secuCodes.add(stockId.trim());
                }
            }

            Map<String, JsonNode> upReasonMap = fetchUpReasonMap(secuCodes);

            List<ClsHotListDay> result = new ArrayList<>();
            int idx = 0;
            for (JsonNode item : list) {
                idx++;
                JsonNode stock = item.path("stock");
                String stockId = asText(stock, "StockID");
                if (stockId == null || stockId.trim().isEmpty()) {
                    continue;
                }
                String name = firstNonBlank(asText(stock, "name"), asText(stock, "Name"));

                ClsHotListDay row = new ClsHotListDay();
                row.setDataDate(queryDate);
                row.setListType(listType);
                row.setStockCode(stockId.trim());
                row.setStockName(name);
                row.setRiseAndFall(asNullableBigDecimal(stock.get("RiseRange")));
                row.setOrderNum(idx);

                ObjectNode raw = item != null && item.isObject() ? (ObjectNode) item.deepCopy() : mapper.createObjectNode();
                JsonNode ur = upReasonMap.get(stockId.trim());
                raw.set("tags", buildTagsForHotStock(item, ur));
                row.setRawJson(mapper.writeValueAsString(raw));
                result.add(row);
            }
            return result;
        }
    }

    private List<ClsHotListDay> fetchFromHotList(LocalDate queryDate, String listType) throws Exception {
        String params = "app=" + CLS_APP + "&os=" + CLS_OS + "&sv=" + CLS_SV;
        String sign = clsSign(params);

        HttpUrl url = HttpUrl.parse(HOT_LIST_URL).newBuilder()
                .query(params + "&sign=" + sign)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "okhttp/4.9.3")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code() + " " + response.message());
            }
            String body = response.body().string();
            JsonNode root = mapper.readTree(body);
            if (root.path("errno").asInt(-1) != 0) {
                throw new RuntimeException("errno=" + root.path("errno").asText() + ", msg=" + root.path("msg").asText(""));
            }
            JsonNode list = root.path("data");
            if (list == null || list.isNull() || !list.isArray()) {
                return new ArrayList<>();
            }

            List<ClsHotListDay> result = new ArrayList<>();
            int idx = 0;
            for (JsonNode item : list) {
                idx++;
                String id = asText(item, "id");
                String title = asText(item, "title");
                if (id == null || id.trim().isEmpty()) {
                    id = String.valueOf(idx);
                }

                ClsHotListDay row = new ClsHotListDay();
                row.setDataDate(queryDate);
                row.setListType(listType);
                row.setStockCode(id);
                row.setStockName(title);
                row.setOrderNum(idx);

                ObjectNode raw = item != null && item.isObject() ? (ObjectNode) item.deepCopy() : mapper.createObjectNode();
                raw.set("tags", buildTagsForHotList(item));
                row.setRawJson(mapper.writeValueAsString(raw));
                result.add(row);
            }
            return result;
        }
    }

    private ArrayNode buildTagsForHotStock(JsonNode item, JsonNode upReason) {
        ArrayNode tags = mapper.createArrayNode();

        String title = asText(item, "title");
        Integer ctype = asNullableInt(item, "ctype");
        if (title != null && !title.trim().isEmpty() && (ctype == null || ctype != 6)) {
            tags.add("热点:" + title.trim());
        }

        if (upReason != null && !upReason.isNull()) {
            Integer upNum = asNullableInt(upReason, "up_num");
            if (upNum != null && upNum > 0) {
                tags.add("连板:" + upNum);
            }

            String reason = asText(upReason, "up_reason");
            if (reason != null && !reason.trim().isEmpty()) {
                tags.add("涨停分析:" + reason.trim());
            }

            JsonNode relPlate = upReason.get("rel_plate");
            if (relPlate != null && relPlate.isArray()) {
                for (JsonNode p : relPlate) {
                    String pn = asText(p, "plate_name");
                    if (pn != null && !pn.trim().isEmpty()) {
                        tags.add(pn.trim());
                    }
                }
            }
        }

        return tags;
    }

    private ArrayNode buildTagsForHotList(JsonNode item) {
        ArrayNode tags = mapper.createArrayNode();
        JsonNode stockList = item.get("stock_list");
        if (stockList != null && stockList.isArray()) {
            int count = 0;
            for (JsonNode s : stockList) {
                String name = asText(s, "name");
                if (name != null && !name.trim().isEmpty()) {
                    tags.add("关联:" + name.trim());
                    count++;
                    if (count >= 3) {
                        break;
                    }
                }
            }
        }
        return tags;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim(), DAY_FMT);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String asText(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText(null);
        if (s == null) {
            return null;
        }
        return s;
    }

    private Integer asNullableInt(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isInt() || v.isLong()) {
            return v.asInt();
        }
        if (v.isTextual()) {
            try {
                return Integer.parseInt(v.asText(""));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private BigDecimal asNullableBigDecimal(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            if (v.isNumber()) {
                return v.decimalValue();
            }
            if (v.isTextual()) {
                String s = v.asText("");
                if (s == null || s.trim().isEmpty()) {
                    return null;
                }
                return new BigDecimal(s.trim());
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private Map<String, JsonNode> fetchUpReasonMap(List<String> secuCodes) throws Exception {
        Map<String, JsonNode> map = new HashMap<>();
        if (secuCodes == null || secuCodes.isEmpty()) {
            return map;
        }

        String joined = String.join(",", secuCodes);
        String params = "app=" + CLS_APP + "&os=" + CLS_OS + "&sv=" + CLS_SV + "&secu_codes=" + joined;
        String sign = clsSign(params);

        HttpUrl url = HttpUrl.parse(UP_REASON_URL).newBuilder()
                .query(params + "&sign=" + sign)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "okhttp/4.9.3")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return map;
            }
            String body = response.body().string();
            JsonNode root = mapper.readTree(body);
            if (root.path("code").asInt(-1) != 200) {
                return map;
            }
            JsonNode data = root.path("data");
            if (data != null && data.isObject()) {
                data.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue()));
            }
            return map;
        }
    }

    private String clsSign(String params) {
        // sign.js: sign(str) = hex_md5(hex_sha1(str))
        String sha1Hex = sha1Hex(params);
        return md5Hex(sha1Hex);
    }

    private String sha1Hex(String s) {
        return digestHex("SHA-1", s);
    }

    private String md5Hex(String s) {
        return digestHex("MD5", s);
    }

    private String digestHex(String algo, String s) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] out = md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }
}
