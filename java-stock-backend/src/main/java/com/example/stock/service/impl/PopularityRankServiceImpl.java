package com.example.stock.service.impl;

import com.example.stock.service.PopularityRankService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PopularityRankServiceImpl implements PopularityRankService {

    private static final Logger log = LoggerFactory.getLogger(PopularityRankServiceImpl.class);

    private static final String DC_RANK_URL = "https://emappdata.eastmoney.com/stockrank/getAllCurrentList";
    private static final String DC_STOCK_INFO_URL_PREFIX = "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&np=3&ut=a79f54e3d4c8d44e494efb8f748db291&invt=2&fields=f2,f3,f12,f14&secids=";
    private static final String RQB_URL = "https://apphq.longhuvip.com/w1/api/index.php?Order=1&a=GetHotPHB&st=60&apiv=w21&Type=1&c=StockBidYiDong&PhoneOSNew=1&VerSion=5";
    private static final String THS_URL = "https://eq.10jqka.com.cn/open/api/hot_list/v1/hot_stock/a/hour/data.txt";
    private static final String DC_MONEY_FLOW_URL = "https://push2.eastmoney.com/api/qt/clist/get?fid=f62&po=1&pz=6000&pn=1&np=1&fltt=2&invt=2&ut=b2884a393a59ad64002292a3e90d46a5&fs=m:0+t:6+f:!2,m:0+t:13+f:!2,m:0+t:80+f:!2,m:1+t:2+f:!2,m:1+t:23+f:!2,m:0+t:7+f:!2,m:1+t:3+f:!2&fields=f12,f14,f2,f3,f62,f184,f66,f69";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public PopularityRankServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> getPopularityRank(Integer limit) {
        Map<String, Object> result = new HashMap<>();
        int pageSize = (limit == null || limit <= 0) ? 100 : limit;

        try {
            List<JsonNode> dcRank = fetchDcRank(pageSize);
            List<JsonNode> dcInfo = dcRank.isEmpty() ? new ArrayList<>() : fetchDcStockInfo(dcRank);
            List<Map<String, Object>> rqbRank = fetchRqbRank();
            List<JsonNode> thsRank = fetchThsRank();
            List<JsonNode> moneyFlow = fetchDcMoneyFlow();

            Map<String, Map<String, Object>> merged = new LinkedHashMap<>();

            for (int i = 0; i < dcInfo.size(); i++) {
                JsonNode stock = dcInfo.get(i);
                String code = asText(stock, "f12");
                if (code == null || code.isEmpty()) {
                    continue;
                }

                String name = asText(stock, "f14");
                double zf = asDouble(stock, "f3");

                int dcpm = i + 1;
                int rqbpm = 1000;
                for (Map<String, Object> r : rqbRank) {
                    if (code.equals(r.get("code"))) {
                        rqbpm = toInt(r.get("ranking"), 1000);
                        break;
                    }
                }

                int thspm = 1000;
                for (JsonNode t : thsRank) {
                    if (code.equals(asText(t, "code"))) {
                        thspm = asInt(t, "order", 1000);
                        break;
                    }
                }

                JsonNode flow = null;
                for (JsonNode f : moneyFlow) {
                    if (code.equals(asText(f, "f12"))) {
                        flow = f;
                        break;
                    }
                }

                Double zlje = flow == null ? null : asNullableDouble(flow, "f62");
                Double zljzb = flow == null ? null : asNullableDouble(flow, "f184");
                Double cddje = flow == null ? null : asNullableDouble(flow, "f66");
                Double cddjzb = flow == null ? null : asNullableDouble(flow, "f69");

                int shy = (dcpm <= 100 && thspm <= 100) ? Math.min(dcpm, thspm) : 1000;
                boolean preselected = shy != 1000 && zf > 0 && zlje != null && zlje >= 20000000 && zljzb != null && zljzb >= 10;

                Map<String, Object> item = new HashMap<>();
                item.put("code", code);
                item.put("name", name);
                item.put("zf", zf);
                item.put("dcpm", dcpm);
                item.put("rqbpm", rqbpm);
                item.put("thspm", thspm);
                item.put("shy", shy);
                item.put("zlje", zlje);
                item.put("zljzb", zljzb);
                item.put("cddje", cddje);
                item.put("cddjzb", cddjzb);
                item.put("preselected", preselected);

                merged.put(code, item);
            }

            for (Map<String, Object> r : rqbRank) {
                String code = (String) r.get("code");
                if (code == null || code.isEmpty() || merged.containsKey(code)) {
                    continue;
                }

                int thspm = 1000;
                for (JsonNode t : thsRank) {
                    if (code.equals(asText(t, "code"))) {
                        thspm = asInt(t, "order", 1000);
                        break;
                    }
                }

                JsonNode flow = null;
                for (JsonNode f : moneyFlow) {
                    if (code.equals(asText(f, "f12"))) {
                        flow = f;
                        break;
                    }
                }

                Double zlje = flow == null ? null : asNullableDouble(flow, "f62");
                Double zljzb = flow == null ? null : asNullableDouble(flow, "f184");
                Double cddje = flow == null ? null : asNullableDouble(flow, "f66");
                Double cddjzb = flow == null ? null : asNullableDouble(flow, "f69");

                Map<String, Object> item = new HashMap<>();
                item.put("code", code);
                item.put("name", r.get("name"));
                item.put("zf", toDouble(r.get("zf"), 0.0));
                item.put("dcpm", 1000);
                item.put("rqbpm", toInt(r.get("ranking"), 1000));
                item.put("thspm", thspm);
                item.put("shy", 1000);
                item.put("zlje", zlje);
                item.put("zljzb", zljzb);
                item.put("cddje", cddje);
                item.put("cddjzb", cddjzb);
                item.put("preselected", false);

                merged.put(code, item);
            }

            for (JsonNode t : thsRank) {
                String code = asText(t, "code");
                if (code == null || code.isEmpty() || merged.containsKey(code)) {
                    continue;
                }

                JsonNode flow = null;
                for (JsonNode f : moneyFlow) {
                    if (code.equals(asText(f, "f12"))) {
                        flow = f;
                        break;
                    }
                }

                Double zlje = flow == null ? null : asNullableDouble(flow, "f62");
                Double zljzb = flow == null ? null : asNullableDouble(flow, "f184");
                Double cddje = flow == null ? null : asNullableDouble(flow, "f66");
                Double cddjzb = flow == null ? null : asNullableDouble(flow, "f69");

                Map<String, Object> item = new HashMap<>();
                item.put("code", code);
                item.put("name", asText(t, "name"));
                item.put("zf", flow == null ? 0.0 : asDouble(flow, "f3"));
                item.put("dcpm", 1000);
                item.put("rqbpm", 1000);
                item.put("thspm", asInt(t, "order", 1000));
                item.put("shy", 1000);
                item.put("zlje", zlje);
                item.put("zljzb", zljzb);
                item.put("cddje", cddje);
                item.put("cddjzb", cddjzb);
                item.put("preselected", false);

                merged.put(code, item);
            }

            List<Map<String, Object>> data = new ArrayList<>(merged.values());
            result.put("success", true);
            result.put("data", data);
            result.put("count", data.size());
            result.put("limit", pageSize);
            Map<String, Object> sources = new HashMap<>();
            sources.put("dcRank", dcRank.size());
            sources.put("dcInfo", dcInfo.size());
            sources.put("rqbRank", rqbRank.size());
            sources.put("thsRank", thsRank.size());
            sources.put("moneyFlow", moneyFlow.size());
            result.put("sources", sources);
        } catch (Exception e) {
            log.error("getPopularityRank failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    private List<JsonNode> fetchDcRank(int pageSize) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("appId", "appId01");
        payload.put("globalId", "786e4c21-70dc-435a-93bb-38");
        payload.put("pageNo", 1);
        payload.put("pageSize", pageSize);
        String bodyJson = mapper.writeValueAsString(payload);

        Request request = baseRequest(DC_RANK_URL)
                .post(RequestBody.create(bodyJson, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ArrayList<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                data.forEach(list::add);
                return list;
            }
            return new ArrayList<>();
        }
    }

    private List<JsonNode> fetchDcStockInfo(List<JsonNode> dcRank) throws Exception {
        StringBuilder secids = new StringBuilder();
        for (JsonNode n : dcRank) {
            String sc = n.has("sc") ? n.get("sc").asText() : "";
            if (sc.isEmpty()) continue;
            String secid = sc.replace("SH", "1.").replace("SZ", "0.");
            if (secids.length() > 0) secids.append(',');
            secids.append(secid);
        }

        String url = DC_STOCK_INFO_URL_PREFIX + secids;
        Request request = baseRequest(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ArrayList<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode diff = root.path("data").path("diff");
            if (diff.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                diff.forEach(list::add);
                return list;
            }
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchRqbRank() throws Exception {
        Request request = baseRequest(RQB_URL).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ArrayList<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode list = root.get("List");
            if (list == null || !list.isArray()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : list) {
                if (!item.isArray() || item.size() < 5) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("code", item.get(0).asText());
                row.put("name", item.get(1).asText());
                row.put("zf", item.get(2).asDouble());
                row.put("bh", item.get(3).asText());
                row.put("ranking", item.get(4).asInt());
                result.add(row);
            }
            return result;
        }
    }

    private List<JsonNode> fetchThsRank() throws Exception {
        Request request = baseRequest(THS_URL).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ArrayList<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode stockList = root.path("data").path("stock_list");
            if (stockList.isArray()) {
                List<JsonNode> result = new ArrayList<>();
                stockList.forEach(result::add);
                return result;
            }
            return new ArrayList<>();
        }
    }

    private List<JsonNode> fetchDcMoneyFlow() throws Exception {
        Request request = baseRequest(DC_MONEY_FLOW_URL).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new ArrayList<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode diff = root.path("data").path("diff");
            if (diff.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                diff.forEach(list::add);
                return list;
            }
            return new ArrayList<>();
        }
    }

    private Request.Builder baseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    }

    private String asText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private double asDouble(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? 0.0 : v.asDouble();
    }

    private Double asNullableDouble(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asDouble();
    }

    private int asInt(JsonNode node, String field, int defaultVal) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? defaultVal : v.asInt(defaultVal);
    }

    private int toInt(Object v, int defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private double toDouble(Object v, double defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
