package com.example.stock.service.impl;

import com.example.stock.dao.ThsHotListDayDao;
import com.example.stock.entity.ThsHotListDay;
import com.example.stock.service.ThsHotListService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThsHotListServiceImpl implements ThsHotListService {

    private static final Logger log = LoggerFactory.getLogger(ThsHotListServiceImpl.class);

    private static final String DAY_URL = "https://dq.10jqka.com.cn/fuyao/hot_list_data/out/hot_list/v1/stock?stock_type=%s&type=day&list_type=%s";

    @Autowired
    private ThsHotListDayDao thsHotListDayDao;

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public ThsHotListServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> syncDayHotList(String date, Boolean force, String stockType, String listType) {
        Map<String, Object> result = new HashMap<>();
        String st = (stockType == null || stockType.trim().isEmpty()) ? "a" : stockType.trim();
        String lt = (listType == null || listType.trim().isEmpty()) ? "normal" : listType.trim();
        String rt = "day";

        LocalDate queryDate = parseDate(date);
        boolean f = force != null && force;

        try {
            if (f) {
                thsHotListDayDao.physicalDelete(queryDate, st, rt, lt);
            }

            int existing = thsHotListDayDao.countByDate(queryDate, st, rt, lt);
            if (existing > 0 && !f) {
                result.put("success", true);
                result.put("date", queryDate.toString());
                result.put("stockType", st);
                result.put("rankType", rt);
                result.put("listType", lt);
                result.put("count", existing);
                result.put("message", "exists");
                return result;
            }

            List<ThsHotListDay> fetched = fetchDayHotListFromApi(queryDate, st, lt);

            thsHotListDayDao.physicalDelete(queryDate, st, rt, lt);
            for (ThsHotListDay row : fetched) {
                thsHotListDayDao.insert(row);
            }

            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("rankType", rt);
            result.put("listType", lt);
            result.put("count", fetched.size());
        } catch (Exception e) {
            log.error("syncDayHotList failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("rankType", rt);
            result.put("listType", lt);
        }

        return result;
    }

    @Override
    public Map<String, Object> listDayHotList(String date, String stockType, String listType) {
        Map<String, Object> result = new HashMap<>();
        String st = (stockType == null || stockType.trim().isEmpty()) ? "a" : stockType.trim();
        String lt = (listType == null || listType.trim().isEmpty()) ? "normal" : listType.trim();
        String rt = "day";

        LocalDate queryDate = parseDate(date);
        try {
            List<ThsHotListDay> list = thsHotListDayDao.findByDate(queryDate, st, rt, lt);
            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("rankType", rt);
            result.put("listType", lt);
            result.put("count", list.size());
            result.put("data", list);
        } catch (Exception e) {
            log.error("listDayHotList failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("rankType", rt);
            result.put("listType", lt);
        }
        return result;
    }

    private List<ThsHotListDay> fetchDayHotListFromApi(LocalDate queryDate, String stockType, String listType) throws Exception {
        String url = String.format(DAY_URL, stockType, listType);
        Request request = baseRequest(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code() + " " + response.message());
            }

            String body = response.body().string();
            if (log.isInfoEnabled()) {
                String snippet = body == null ? "" : (body.length() > 300 ? body.substring(0, 300) : body);
                log.info("THS hot list http={}, body.len={}, body.head={}", response.code(), body == null ? 0 : body.length(), snippet);
            }
            if (log.isWarnEnabled()) {
                String snippet = body == null ? "" : (body.length() > 500 ? body.substring(0, 500) : body);
                log.warn("THS hot list raw head={}", snippet);
            }
            JsonNode root = mapper.readTree(body);
            JsonNode dataNode = root.get("data");

            JsonNode list = root.at("/data/list");
            if (list.isMissingNode() || list.isNull()) {
                list = root.path("data").path("list");
            }

            if (list == null || list.isMissingNode() || list.isNull()) {
                // Observed real response uses data.stock_list
                list = root.at("/data/stock_list");
            }
            if (list == null || list.isMissingNode() || list.isNull()) {
                list = root.at("/data/stock_list/list");
            }

            if (list != null && list.isTextual()) {
                try {
                    list = mapper.readTree(list.asText("")).path("list");
                } catch (Exception ignore) {
                    // ignore
                }
            }

            if (list != null && list.isObject()) {
                JsonNode inner = list.get("list");
                if (inner != null) {
                    list = inner;
                }
            }

            if (list != null && list.isObject()) {
                // Some responses may encode list as an object (e.g. {"0": {...}, "1": {...}})
                // Convert object values to an array for unified processing.
                com.fasterxml.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
                list.elements().forEachRemaining(arr::add);
                list = arr;
            }

            if (list == null || list.isMissingNode() || list.isNull() || !list.isArray()) {
                String statusMsg = root.path("status_msg").asText("");
                String listNodeType = list == null ? "null" : list.getNodeType().toString();
                String dataKeys = "";
                if (dataNode != null && dataNode.isObject()) {
                    List<String> keys = new ArrayList<>();
                    dataNode.fieldNames().forEachRemaining(keys::add);
                    dataKeys = keys.toString();
                }
                log.warn("THS hot list response has no array list node. status_msg={}, listType={}, data.nodeType={}",
                        statusMsg,
                        listNodeType,
                        dataNode == null ? "null" : dataNode.getNodeType());
                if (!dataKeys.isEmpty()) {
                    log.warn("THS hot list data keys={}", dataKeys);
                }
                return new ArrayList<>();
            }

            List<ThsHotListDay> result = new ArrayList<>();
            for (JsonNode item : list) {
                ThsHotListDay row = new ThsHotListDay();
                row.setDataDate(queryDate);
                row.setStockType(stockType);
                row.setRankType("day");
                row.setListType(listType);

                row.setMarket(asNullableInt(item, "market"));
                row.setStockCode(asText(item, "code"));
                row.setStockName(asText(item, "name"));

                row.setRate(asNullableBigDecimal(item.get("rate")));
                row.setRiseAndFall(asNullableBigDecimal(item.get("rise_and_fall")));

                row.setAnalyseTitle(asText(item, "analyse_title"));
                row.setAnalyse(asText(item, "analyse"));
                row.setHotRankChg(asNullableInt(item, "hot_rank_chg"));

                JsonNode topic = item.get("topic");
                if (topic != null && !topic.isNull()) {
                    row.setTopicCode(asText(topic, "topic_code"));
                    row.setTopicTitle(asText(topic, "title"));
                    row.setTopicIosJumpUrl(asText(topic, "ios_jump_url"));
                    row.setTopicAndroidJumpUrl(asText(topic, "android_jump_url"));
                }

                JsonNode tag = item.get("tag");
                if (tag != null && !tag.isNull()) {
                    JsonNode concept = tag.get("concept_tag");
                    if (concept != null && concept.isArray()) {
                        row.setConceptTags(mapper.writeValueAsString(concept));
                    }
                    row.setPopularityTag(asText(tag, "popularity_tag"));
                }

                row.setOrderNum(asNullableInt(item, "order"));
                row.setRawJson(mapper.writeValueAsString(item));

                if (row.getStockCode() == null || row.getStockCode().trim().isEmpty()) {
                    continue;
                }

                result.add(row);
            }
            return result;
        }
    }

    private Request.Builder baseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("referer", "https://eq.10jqka.com.cn/webpage/ths-hot-list/index.html")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        String d = date.trim();
        if (d.matches("\\d{8}")) {
            return LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return LocalDate.now();
    }

    private String asText(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) {
            return "";
        }
        return v.asText("");
    }

    private Integer asNullableInt(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isInt() || v.isLong()) {
            return v.asInt();
        }
        String s = v.asText("").trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal asNullableBigDecimal(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return BigDecimal.valueOf(v.asDouble());
        }
        String s = v.asText("").trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }
}
