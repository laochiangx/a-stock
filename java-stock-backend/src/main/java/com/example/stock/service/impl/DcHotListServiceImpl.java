package com.example.stock.service.impl;

import com.example.stock.dao.DcHotListDayDao;
import com.example.stock.dao.DongcaiThemePlateDao;
import com.example.stock.dao.StockDailySnapshotDao;
import com.example.stock.dao.StockDailySnapshotTagDao;
import com.example.stock.dao.TagsDao;
import com.example.stock.entity.DcHotListDay;
import com.example.stock.entity.StockDailySnapshot;
import com.example.stock.entity.StockDailySnapshotTag;
import com.example.stock.entity.Tags;
import com.example.stock.service.DcHotListService;
import com.example.stock.service.StockTaggingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DcHotListServiceImpl implements DcHotListService {

    private static final Logger log = LoggerFactory.getLogger(DcHotListServiceImpl.class);

    private static final String DC_RANK_URL = "https://emappdata.eastmoney.com/stockrank/getAllCurrentList";
    private static final String DC_STOCK_INFO_URL_PREFIX = "https://push2.eastmoney.com/api/qt/ulist.np/get?ut=f057cbcbce2a86e2866ab8877db1d059&fltt=2&invt=2&fields="
            + "f14,f148,f3,f12,f2,f13,f29"
            + "&secids=";

    private static final String DC_DATACENTER_URL = "https://datacenter.eastmoney.com/securities/api/data/v1/get";
    private static final String DC_APISTOCK_TRAN_URL = "https://emstockdiag.eastmoney.com/apistock/Tran/GetData";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Autowired
    private DcHotListDayDao dcHotListDayDao;

    @Autowired(required = false)
    private DongcaiThemePlateDao dongcaiThemePlateDao;

    @Autowired(required = false)
    private StockTaggingService stockTaggingService;

    @Autowired(required = false)
    private StockDailySnapshotDao stockDailySnapshotDao;

    @Autowired(required = false)
    private StockDailySnapshotTagDao stockDailySnapshotTagDao;

    @Autowired(required = false)
    private TagsDao tagsDao;

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public DcHotListServiceImpl() {
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
            if (stockTaggingService != null) {
                try {
                    stockTaggingService.tagByDate(queryDate);
                } catch (Exception e) {
                    log.warn("stockTaggingService.tagByDate failed, date={}", queryDate);
                }
            }

            if (f) {
                dcHotListDayDao.physicalDelete(queryDate, lt);
            }

            int existing = dcHotListDayDao.countByDate(queryDate, lt);
            if (existing > 0 && !f) {
                result.put("success", true);
                result.put("date", queryDate.toString());
                result.put("listType", lt);
                result.put("count", existing);
                result.put("message", "exists");
                return result;
            }

            List<DcHotListDay> fetched = fetchDayHotListFromApi(queryDate, lt, 100);

            dcHotListDayDao.physicalDelete(queryDate, lt);
            for (DcHotListDay row : fetched) {
                dcHotListDayDao.insert(row);
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

    private List<String> fetchThemePlates(LocalDate queryDate, String stockCode) {
        if (dongcaiThemePlateDao == null) {
            return new ArrayList<>();
        }
        try {
            List<String> list = dongcaiThemePlateDao.findPlateNamesByDateAndStockCode(queryDate, stockCode);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ObjectNode fetchTagSystem(LocalDate queryDate, String stockCode, Set<String> excludeNames) {
        if (stockDailySnapshotDao == null || stockDailySnapshotTagDao == null || tagsDao == null) {
            return null;
        }

        try {
            StockDailySnapshot snapshot = stockDailySnapshotDao.selectOne(
                    new LambdaQueryWrapper<StockDailySnapshot>()
                            .eq(StockDailySnapshot::getDataDate, queryDate)
                            .eq(StockDailySnapshot::getStockCode, stockCode)
            );
            if (snapshot == null || snapshot.getId() == null) {
                return null;
            }

            List<StockDailySnapshotTag> rels = stockDailySnapshotTagDao.selectList(
                    new LambdaQueryWrapper<StockDailySnapshotTag>()
                            .eq(StockDailySnapshotTag::getSnapshotId, snapshot.getId())
            );
            if (rels == null || rels.isEmpty()) {
                return null;
            }

            List<Long> ids = new ArrayList<>();
            for (StockDailySnapshotTag r : rels) {
                if (r != null && r.getTagId() != null) {
                    ids.add(r.getTagId());
                }
            }
            if (ids.isEmpty()) {
                return null;
            }

            List<Tags> tags = tagsDao.selectBatchIds(ids);
            if (tags == null || tags.isEmpty()) {
                return null;
            }
            tags.sort((a, b) -> {
                int pa = a == null || a.getPriority() == null ? 0 : a.getPriority();
                int pb = b == null || b.getPriority() == null ? 0 : b.getPriority();
                return Integer.compare(pb, pa);
            });

            ArrayNode theme = mapper.createArrayNode();
            ArrayNode signal = mapper.createArrayNode();
            ArrayNode source = mapper.createArrayNode();
            ArrayNode other = mapper.createArrayNode();

            Set<String> exclude = excludeNames == null ? new LinkedHashSet<>() : excludeNames;
            for (Tags t : tags) {
                if (t == null) continue;
                String name = t.getName();
                if (name == null) continue;
                String n = name.trim();
                if (n.isEmpty() || exclude.contains(n)) {
                    continue;
                }

                String type = t.getType() == null ? "" : t.getType().trim();
                if ("theme".equalsIgnoreCase(type)) {
                    theme.add(n);
                } else if ("signal".equalsIgnoreCase(type)) {
                    signal.add(n);
                } else if ("source".equalsIgnoreCase(type)) {
                    source.add(n);
                } else {
                    other.add(n);
                }
            }

            ObjectNode ts = mapper.createObjectNode();
            if (theme.size() > 0) ts.set("theme", theme);
            if (signal.size() > 0) ts.set("signal", signal);
            if (source.size() > 0) ts.set("source", source);
            if (other.size() > 0) ts.set("other", other);
            if (ts.size() == 0) {
                return null;
            }
            return ts;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> listDayHotList(String date, String listType) {
        Map<String, Object> result = new HashMap<>();
        String lt = (listType == null || listType.trim().isEmpty()) ? "stock" : listType.trim();
        LocalDate queryDate = parseDate(date);

        try {
            List<DcHotListDay> list = dcHotListDayDao.findByDate(queryDate, lt);
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

    private List<DcHotListDay> fetchDayHotListFromApi(LocalDate queryDate, String listType, int pageSize) throws Exception {
        List<JsonNode> rankList = fetchDcRank(pageSize);
        Map<String, JsonNode> infoMap = rankList.isEmpty() ? new HashMap<>() : fetchDcStockInfoMap(rankList);

        List<String> codes = new ArrayList<>();
        for (JsonNode item : rankList) {
            String sc = asText(item, "sc");
            String code = normalizeCode(sc, asText(item, "code"));
            if (code != null && !code.trim().isEmpty()) {
                codes.add(code.trim());
            }
        }
        Map<String, List<String>> uiLabelsMap = codes.isEmpty() ? new HashMap<>() : fetchUiLabelsMap(codes);
        Map<String, ArrayNode> hotAnalysisMap = codes.isEmpty() ? new HashMap<>() : fetchHotAnalysisMap(codes);

        List<DcHotListDay> result = new ArrayList<>();
        int order = 0;
        for (JsonNode item : rankList) {
            order++;
            String sc = asText(item, "sc");
            String code = normalizeCode(sc, asText(item, "code"));
            if (code == null || code.trim().isEmpty()) {
                continue;
            }

            JsonNode info = infoMap.get(code);

            DcHotListDay row = new DcHotListDay();
            row.setDataDate(queryDate);
            row.setListType(listType);
            row.setStockCode(code);
            row.setStockName(firstNonEmpty(
                    asText(item, "n"),
                    asText(item, "sn"),
                    asText(item, "name"),
                    info == null ? "" : asText(info, "f14")
            ));
            int rk = item.has("rk") ? item.path("rk").asInt(0) : 0;
            row.setOrderNum(rk > 0 ? rk : order);

            BigDecimal hotScore = firstBigDecimal(item,
                    "rc",
                    "hot",
                    "hotScore",
                    "hot_score",
                    "score",
                    "rankScore",
                    "rate"
            );
            row.setHotScore(hotScore);

            BigDecimal zf = info == null ? null : asNullableBigDecimal(info.get("f3"));
            row.setRiseAndFall(zf);

            ObjectNode merged = mapper.createObjectNode();
            merged.set("rank", item);
            if (info != null) {
                merged.set("quote", info);
            }

            List<String> themePlates = fetchThemePlates(queryDate, code);
            Set<String> themePlateSet = new LinkedHashSet<>();
            if (themePlates != null && !themePlates.isEmpty()) {
                ObjectNode tags;
                JsonNode tagsNode = merged.get("tags");
                if (tagsNode != null && tagsNode.isObject()) {
                    tags = (ObjectNode) tagsNode;
                } else {
                    tags = mapper.createObjectNode();
                }
                ArrayNode arr = mapper.createArrayNode();
                for (String t : themePlates) {
                    if (t == null) continue;
                    String s = t.trim();
                    if (!s.isEmpty()) {
                        themePlateSet.add(s);
                        arr.add(s);
                    }
                }
                if (arr.size() > 0) {
                    tags.set("themePlates", arr);
                    merged.set("tags", tags);
                }
            }

            ObjectNode tagSystem = fetchTagSystem(queryDate, code, themePlateSet);
            if (tagSystem != null && tagSystem.size() > 0) {
                ObjectNode tags;
                JsonNode tagsNode = merged.get("tags");
                if (tagsNode != null && tagsNode.isObject()) {
                    tags = (ObjectNode) tagsNode;
                } else {
                    tags = mapper.createObjectNode();
                }
                tags.set("tagSystem", tagSystem);
                merged.set("tags", tags);
            }

            List<String> uiLabels = uiLabelsMap.get(code);
            List<String> quoteLabels = parseQuoteUiLabels(info);
            Set<String> mergedLabels = new LinkedHashSet<>();
            if (uiLabels != null && !uiLabels.isEmpty()) {
                mergedLabels.addAll(uiLabels);
            }
            if (quoteLabels != null && !quoteLabels.isEmpty()) {
                mergedLabels.addAll(quoteLabels);
            }
            if (!mergedLabels.isEmpty()) {
                ObjectNode tags;
                JsonNode tagsNode = merged.get("tags");
                if (tagsNode != null && tagsNode.isObject()) {
                    tags = (ObjectNode) tagsNode;
                } else {
                    tags = mapper.createObjectNode();
                }
                ArrayNode arr = mapper.createArrayNode();
                for (String t : mergedLabels) {
                    if (t == null) continue;
                    String s = t.trim();
                    if (!s.isEmpty()) {
                        arr.add(s);
                    }
                }
                if (arr.size() > 0) {
                    tags.set("uiLabels", arr);
                    merged.set("tags", tags);
                }
            }

            ArrayNode hotAnalysis = hotAnalysisMap.get(code);
            if (hotAnalysis != null && hotAnalysis.size() > 0) {
                ObjectNode tags;
                JsonNode tagsNode = merged.get("tags");
                if (tagsNode != null && tagsNode.isObject()) {
                    tags = (ObjectNode) tagsNode;
                } else {
                    tags = mapper.createObjectNode();
                }
                tags.set("hotAnalysis", hotAnalysis);
                merged.set("tags", tags);
            }
            row.setRawJson(mapper.writeValueAsString(merged));

            result.add(row);
        }

        return result;
    }

    private List<JsonNode> fetchDcRank(int pageSize) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("appId", "appId01");
        payload.put("globalId", "786e4c21-70dc-435a-93bb-38");
        payload.put("marketType", "");
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

    private Map<String, JsonNode> fetchDcStockInfoMap(List<JsonNode> dcRank) throws Exception {
        StringBuilder secids = new StringBuilder();
        for (JsonNode n : dcRank) {
            String sc = n.has("sc") ? n.get("sc").asText() : "";
            if (sc.isEmpty()) continue;
            String secid = sc.replace("SH", "1.").replace("SZ", "0.");
            if (secids.length() > 0) secids.append(',');
            secids.append(secid);
        }

        if (secids.length() == 0) {
            return new HashMap<>();
        }

        String url = DC_STOCK_INFO_URL_PREFIX + secids;
        Request request = baseRequest(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new HashMap<>();
            }
            JsonNode root = mapper.readTree(response.body().string());
            JsonNode diff = root.path("data").path("diff");
            if (!diff.isArray()) {
                return new HashMap<>();
            }

            Map<String, JsonNode> result = new HashMap<>();
            for (JsonNode d : diff) {
                String code = asText(d, "f12");
                if (code == null || code.isEmpty()) continue;
                result.put(code, d);
            }
            return result;
        }
    }

    private Request.Builder baseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "*/*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("origin", "https://vipmoney.eastmoney.com")
                .addHeader("referer", "https://vipmoney.eastmoney.com/")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
    }

    private String toSecurityCodeMk(String code) {
        if (code == null) return null;
        String c = code.trim();
        if (c.isEmpty()) return null;
        if (c.startsWith("SH") || c.startsWith("SZ")) {
            return c;
        }
        if (!c.matches("\\d{6}")) {
            return null;
        }
        if (c.startsWith("6")) {
            return "SH" + c;
        }
        if (c.startsWith("0") || c.startsWith("3")) {
            return "SZ" + c;
        }
        return null;
    }

    private Map<String, List<String>> fetchUiLabelsMap(List<String> codes) {
        Map<String, List<String>> result = new HashMap<>();
        if (codes == null || codes.isEmpty()) {
            return result;
        }

        List<String> mkCodes = new ArrayList<>();
        for (String c : codes) {
            String mk = toSecurityCodeMk(c);
            if (mk != null) {
                mkCodes.add(mk);
            }
        }
        if (mkCodes.isEmpty()) {
            return result;
        }

        StringBuilder filterSb = new StringBuilder();
        filterSb.append("(SECURITY_CODE_MK in (").append("\"");
        for (int i = 0; i < mkCodes.size(); i++) {
            if (i > 0) filterSb.append(",");
            filterSb.append(mkCodes.get(i));
        }
        filterSb.append("\"").append("))");
        String filter = filterSb.toString();

        List<JsonNode> allRows = new ArrayList<>();
        allRows.addAll(fetchDatacenterRows("RPT_CUSTOM_INDIVIDUAL_SHARE_DIMENSIONALITY_GG", filter, 500));
        allRows.addAll(fetchDatacenterRows("RPT_CUSTOM_INDIVIDUAL_SHARE_DIMENSIONALITY_BK", filter, 500));

        Set<String> allUiLabels = new LinkedHashSet<>();
        for (JsonNode row : allRows) {
            String mk = firstNonEmpty(asText(row, "SECURITY_CODE_MK"), asText(row, "SECURITY_CODE"), asText(row, "SECUCODE"));
            String code = mk;
            if (code != null) {
                code = code.replaceAll("[^0-9]", "");
            }
            if (code == null || code.isEmpty()) {
                continue;
            }

            String label = firstNonEmpty(asText(row, "INDICATOR_NAME"), asText(row, "BOARD_NAME"));
            if (label == null || label.trim().isEmpty()) {
                continue;
            }
            String l = label.trim();

            List<String> list = result.computeIfAbsent(code, k -> new ArrayList<>());
            if (!list.contains(l)) {
                list.add(l);
            }
            allUiLabels.add(l);
        }

        if (tagsDao != null && !allUiLabels.isEmpty()) {
            for (String name : allUiLabels) {
                try {
                    Tags exist = tagsDao.selectOne(
                            new LambdaQueryWrapper<Tags>()
                                    .eq(Tags::getName, name)
                                    .eq(Tags::getType, "ui")
                    );
                    if (exist == null) {
                        Tags t = new Tags();
                        t.setName(name);
                        t.setType("ui");
                        t.setPriority(10);
                        tagsDao.insert(t);
                    }

                } catch (Exception ignore) {
                }
            }
        }

        return result;
    }

    private List<JsonNode> fetchDatacenterRows(String reportName, String filter, int pageSize) {
        List<JsonNode> list = new ArrayList<>();
        try {
            HttpUrl url = HttpUrl.parse(DC_DATACENTER_URL).newBuilder()
                    .addQueryParameter("reportName", reportName)
                    .addQueryParameter("filter", filter)
                    .addQueryParameter("pageNumber", "1")
                    .addQueryParameter("pageSize", String.valueOf(pageSize))
                    .addQueryParameter("sortColumns", "SECUCODE,CLASSIF_CODE,INDICATOR_NUM")
                    .addQueryParameter("sortTypes", "1,1,1")
                    .addQueryParameter("source", "SECURITIES")
                    .addQueryParameter("client", "APP")
                    .addQueryParameter("v", String.valueOf(System.currentTimeMillis()))
                    .build();

            Request request = baseRequest(url.toString()).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return list;
                }
                JsonNode root = mapper.readTree(response.body().string());
                JsonNode data = root.at("/result/data");
                if (data.isMissingNode() || data.isNull()) {
                    data = root.path("result").path("data");
                }
                if (data != null && data.isArray()) {
                    for (JsonNode n : data) {
                        list.add(n);
                    }
                }
            }
        } catch (Exception e) {
            return list;
        }
        return list;
    }

    private Map<String, ArrayNode> fetchHotAnalysisMap(List<String> codes) {
        Map<String, ArrayNode> result = new HashMap<>();
        if (codes == null || codes.isEmpty()) {
            return result;
        }

        List<String> gubaCodes = new ArrayList<>();
        for (String c : codes) {
            String mk = toSecurityCodeMk(c);
            if (mk != null) {
                gubaCodes.add(mk);
            }
        }
        if (gubaCodes.isEmpty()) {
            return result;
        }

        String joined = String.join(",", gubaCodes);
        ObjectNode parm = mapper.createObjectNode();
        parm.put("deviceid", "C1C1C88D75DF3F58430A09741E1681C5");
        parm.put("version", "180");
        parm.put("product", "EastMoney");
        parm.put("plat", "Android");
        parm.put("gubaCode", joined);

        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("path", "newtopic/api/Topic/GubaCodeHotTopicNewRead");
            payload.put("parm", mapper.writeValueAsString(parm));
            payload.put("track", "tanzhen_sys_" + System.currentTimeMillis());
            payload.put("pageUrl", "https://vipmoney.eastmoney.com/collect/app_ranking/ranking/app.html#/stock");

            Request request = new Request.Builder()
                    .url(DC_APISTOCK_TRAN_URL)
                    .addHeader("accept", "application/json, text/plain, */*")
                    .addHeader("accept-language", "zh-CN,zh;q=0.9")
                    .addHeader("content-type", "application/json")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .post(RequestBody.create(mapper.writeValueAsString(payload), JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return result;
                }
                String body = response.body().string();
                JsonNode root = mapper.readTree(body);

                JsonNode parsed = root;
                if (root.has("data") && root.get("data").isTextual()) {
                    try {
                        String decoded = URLDecoder.decode(root.get("data").asText(""), StandardCharsets.UTF_8);
                        parsed = mapper.readTree(decoded);
                    } catch (Exception ignore) {
                    }
                }

                JsonNode re = parsed.at("/data/re");
                if (re.isMissingNode() || re.isNull()) {
                    re = parsed.path("data").path("re");
                }
                if (re.isMissingNode() || re.isNull()) {
                    re = parsed.path("re");
                }
                if (re == null || re.isMissingNode() || re.isNull() || !re.isObject()) {
                    return result;
                }

                for (String code : codes) {
                    String mk = toSecurityCodeMk(code);
                    if (mk == null) continue;
                    JsonNode arrNode = re.get(mk);
                    if (arrNode == null || arrNode.isNull() || !arrNode.isArray()) {
                        continue;
                    }
                    ArrayNode arr = mapper.createArrayNode();
                    for (JsonNode it : arrNode) {
                        if (it == null || it.isNull()) continue;
                        String title = asText(it, "name");
                        String summary = asText(it, "summary");
                        String id = asText(it, "htid");
                        if ((title == null || title.trim().isEmpty()) && (summary == null || summary.trim().isEmpty())) {
                            continue;
                        }
                        ObjectNode one = mapper.createObjectNode();
                        if (title != null && !title.trim().isEmpty()) one.put("title", title.trim());
                        if (summary != null && !summary.trim().isEmpty()) one.put("summary", summary.trim());
                        if (id != null && !id.trim().isEmpty()) one.put("id", id.trim());
                        arr.add(one);
                    }
                    if (arr.size() > 0) {
                        result.put(code, arr);
                    }
                }
            }
        } catch (Exception e) {
            return result;
        }

        return result;
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

    private String normalizeCode(String sc, String fallback) {
        String code = "";
        if (sc != null) {
            code = sc.trim();
            if (code.startsWith("SH") || code.startsWith("SZ")) {
                code = code.substring(2);
            }
        }
        if (code.isEmpty() && fallback != null) {
            code = fallback.trim();
            if (code.startsWith("SH") || code.startsWith("SZ")) {
                code = code.substring(2);
            }
        }
        return code;
    }

    private String firstNonEmpty(String... arr) {
        if (arr == null) return "";
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) {
                return s.trim();
            }
        }
        return "";
    }

    private BigDecimal firstBigDecimal(JsonNode node, String... fields) {
        if (node == null || fields == null) return null;
        for (String f : fields) {
            JsonNode v = node.get(f);
            BigDecimal bd = asNullableBigDecimal(v);
            if (bd != null) {
                return bd;
            }
        }
        return null;
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

    private List<String> parseQuoteUiLabels(JsonNode quote) {
        List<String> list = new ArrayList<>();
        if (quote == null || quote.isNull()) {
            return list;
        }

        try {
            JsonNode v = quote.get("f148");
            if (v == null || v.isNull()) {
                return list;
            }

            if (v.isArray()) {
                for (JsonNode it : v) {
                    if (it == null || it.isNull()) continue;
                    String s = it.asText("").trim();
                    if (!s.isEmpty()) {
                        list.add(s);
                    }
                }
                return list;
            }

            String raw = v.asText("").trim();
            if (raw.isEmpty()) {
                return list;
            }

            // f148 may contain delimited labels; split conservatively.
            String cleaned = raw.replace("\u0000", " ").trim();
            String[] parts = cleaned.split("[\\|,;\\s、/]+",
                    -1);
            for (String p : parts) {
                if (p == null) continue;
                String s = p.trim();
                if (s.isEmpty()) continue;
                if (s.length() > 50) continue;
                list.add(s);
            }
        } catch (Exception ignore) {
            return list;
        }

        return list;
    }
}
