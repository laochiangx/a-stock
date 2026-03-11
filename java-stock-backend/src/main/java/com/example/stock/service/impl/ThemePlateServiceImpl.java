package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.ThemePlateDataDao;
import com.example.stock.dao.ThemePlateSummaryDao;
import com.example.stock.dao.XuangutongThemePlateDao;
import com.example.stock.dao.DongcaiThemePlateDao;
import com.example.stock.dao.WuyangThemeSubjectDao;
import com.example.stock.entity.ThemePlateData;
import com.example.stock.entity.ThemePlateSummary;
import com.example.stock.entity.XuangutongThemePlate;
import com.example.stock.entity.DongcaiThemePlate;
import com.example.stock.entity.WuyangThemeSubject;
import com.example.stock.service.ThemePlateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ThemePlateServiceImpl implements ThemePlateService {

    private static final Logger log = LoggerFactory.getLogger(ThemePlateServiceImpl.class);
    
    private static final String BASE_URL = "https://flash-api.xuangubao.com.cn/api/surge_stock";
    private static final String PLATES_URL = BASE_URL + "/plates";
    private static final String STOCKS_URL = BASE_URL + "/stocks?normal=true&uplimit=true";
    private static final String DONGCAI_PLATES_URL = "https://flash-api.xuangubao.cn/api/surge_stock/plates";
    private static final String DONGCAI_STOCKS_URL = "https://flash-api.xuangubao.cn/api/surge_stock/stocks?normal=true&uplimit=true";

    private static final String WUYANG_REPLAYROBOT_JSON_URL_TEMPLATE = "https://www.wuylh.com/replayrobot/json/%sp.json";
    private static final String WUYANG_RECENTLY_TRADE_DATE_URL = "https://www.wuylh.com/jihehelper/api/recentlyTradeDate";

    private static final String DC_VIP_HOT_BD_URL = "https://emcfgdata.eastmoney.com/api/themeInvest/getHotBD";
    private static final String DC_VIP_THEME_STOCK_LIST_URL = "https://emcfgdata.eastmoney.com/api/themeInvest/getStockList";
    private static final String DC_VIP_DATACENTER_URL = "https://datacenter.eastmoney.com/securities/api/data/v1/get";
    private static final String DC_PUSH2_BOARD_STOCKS_URL = "https://push2.eastmoney.com/api/qt/clist/get";
    private static final String DC_PUSH2_UT = "b2884a393a59ad64002292a3e90d46a5";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // 交易时间配置
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 0);
    
    // 数据库是否可用的标志
    private volatile boolean dbAvailable = true;

    private volatile boolean dcHotThemeStockDayTableEnsured = false;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private ThemePlateDataDao themePlateDataDao;
    
    @Autowired
    private ThemePlateSummaryDao themePlateSummaryDao;
    
    @Autowired
    private XuangutongThemePlateDao xuangutongThemePlateDao;
    
    @Autowired
    private DongcaiThemePlateDao dongcaiThemePlateDao;

    @Autowired
    private WuyangThemeSubjectDao wuyangThemeSubjectDao;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public ThemePlateServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 检查数据库是否可用
     */
    private boolean isDbAvailable() {
        if (!dbAvailable) {
            return false;
        }
        return themePlateDataDao != null && themePlateSummaryDao != null;
    }
    
    /**
     * 标记数据库不可用
     */
    private void markDbUnavailable() {
        if (dbAvailable) {
            dbAvailable = false;
            log.warn("数据库连接失败，后续请求将跳过数据库操作");
        }
    }
    
    /**
     * 判断是否是交易日（简单判断：周一到周五）
     * 实际应用中可以接入交易日历API
     */
    private boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    /**
     * 判断当前是否是开盘时间
     */
    private boolean isMarketOpen() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (!isTradingDay(today)) {
            return false;
        }
        
        // 9:30 - 15:00 为开盘时间
        return !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
    }
    
    /**
     * 判断是否是交易日收盘后（15:00之后）
     */
    private boolean isAfterMarketClose() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (!isTradingDay(today)) {
            return false;
        }
        
        return now.isAfter(MARKET_CLOSE);
    }
    
    /**
     * 判断是否应该从API获取数据
     * - 开盘时间：总是从API获取
     * - 收盘后：如果数据库没有数据则从API获取，否则用缓存
     * - 非交易日：只用缓存
     */
    private boolean shouldFetchFromApi(LocalDate queryDate) {
        LocalDate today = LocalDate.now();
        
        // 查询的不是今天，只能用缓存
        if (!queryDate.equals(today)) {
            return false;
        }
        
        // 开盘时间，总是从API获取实时数据
        if (isMarketOpen()) {
            log.info("当前是开盘时间，从API获取实时数据");
            return true;
        }
        
        // 数据库不可用，直接从API获取
        if (!isDbAvailable()) {
            log.info("数据库不可用，从API获取数据");
            return true;
        }
        
        // 交易日收盘后，检查数据库是否有数据
        if (isAfterMarketClose()) {
            try {
                int count = themePlateDataDao.countByDate(today);
                if (count == 0) {
                    log.info("交易日收盘后，数据库无数据，从API获取");
                    return true;
                } else {
                    log.info("交易日收盘后，使用数据库缓存，共{}条记录", count);
                    return false;
                }
            } catch (Exception e) {
                log.warn("数据库查询失败，从API获取: {}", e.getMessage());
                markDbUnavailable();
                return true;
            }
        }
        
        // 非交易日或盘前，检查数据库
        try {
            int count = themePlateDataDao.countByDate(today);
            if (count > 0) {
                log.info("非交易时间，使用数据库缓存");
                return false;
            }
        } catch (Exception e) {
            log.warn("数据库查询失败: {}", e.getMessage());
            markDbUnavailable();
        }
        
        // 非交易日且无缓存，尝试从API获取
        return isTradingDay(today);
    }

    private Request.Builder createRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8")
                .addHeader("origin", "https://xuangutong.com.cn")
                .addHeader("referer", "https://xuangutong.com.cn/top-gainer")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    }

    private Request.Builder createVipmoneyRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "*/*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("content-type", "application/json")
                .addHeader("origin", "https://vipmoney.eastmoney.com")
                .addHeader("referer", "https://vipmoney.eastmoney.com/")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
    }

    private Request.Builder createEmwapRequestBuilder(String url, String boardCode) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("origin", "https://emwap.eastmoney.com")
                .addHeader("referer", "https://emwap.eastmoney.com/quote/stock/90." + boardCode + ".html")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
    }

    @Override
    public Map<String, Object> getPlates(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        LocalDate today = LocalDate.now();
        
        try {
            // 开盘时间：直接从API获取实时数据，避免返回数据库缓存的旧数据
            if (queryDate.equals(today) && isMarketOpen()) {
                log.info("开盘时间从API获取选股通板块数据，日期: {}", queryDate);
                Request request = createRequestBuilder(PLATES_URL).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        result.put("success", false);
                        result.put("message", "API请求失败");
                        result.put("date", queryDate.toString());
                        return result;
                    }

                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);

                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> plates = new ArrayList<>();
                        JsonNode data = root.get("data");
                        JsonNode items = data != null && data.has("items") ? data.get("items") : data;

                        if (items != null && items.isArray()) {
                            for (JsonNode item : items) {
                                Map<String, Object> plate = new HashMap<>();
                                plate.put("id", item.has("id") ? item.get("id").asText() : "");
                                plate.put("name", item.has("name") ? item.get("name").asText() : "");
                                plate.put("description", item.has("description") ? item.get("description").asText() : "");
                                plates.add(plate);
                            }
                        }

                        result.put("success", true);
                        result.put("data", plates);
                        result.put("date", queryDate.toString());
                        result.put("fromCache", false);
                        return result;
                    } else {
                        result.put("success", false);
                        result.put("message", "API返回错误");
                        result.put("date", queryDate.toString());
                        return result;
                    }
                }
            }

            // 先从数据库查询（如果数据库可用）
            if (isDbAvailable()) {
                try {
                    List<ThemePlateSummary> dbData = themePlateSummaryDao.findByDate(queryDate);
                    if (!dbData.isEmpty()) {
                        log.info("从数据库获取板块数据，日期: {}, 数量: {}", queryDate, dbData.size());
                        List<Map<String, Object>> plates = dbData.stream().map(s -> {
                            Map<String, Object> plate = new HashMap<>();
                            plate.put("id", s.getPlateId());
                            plate.put("name", s.getPlateName());
                            plate.put("description", s.getPlateDescription());
                            return plate;
                        }).collect(Collectors.toList());
                        
                        result.put("success", true);
                        result.put("data", plates);
                        result.put("date", queryDate.toString());
                        result.put("fromCache", true);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("数据库查询失败: {}", e.getMessage());
                    markDbUnavailable();
                }
            }
            
            if (!queryDate.equals(today)) {
                result.put("success", false);
                result.put("message", "历史日期无缓存，请先在当日同步入库");
                result.put("date", queryDate.toString());
                result.put("fromCache", true);
                return result;
            }

            // 数据库无数据，尝试从API获取（选股通板块）
            log.info("数据库无选股通板块数据，尝试从API获取，日期: {}", queryDate);
            
            // 从API获取
            Request request = createRequestBuilder(PLATES_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    result.put("date", queryDate.toString());
                    return result;
                }

                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                
                if (root.has("code") && root.get("code").asInt() == 20000) {
                    List<Map<String, Object>> plates = new ArrayList<>();
                    JsonNode data = root.get("data");
                    JsonNode items = data != null && data.has("items") ? data.get("items") : data;
                    
                    if (items != null && items.isArray()) {
                        for (JsonNode item : items) {
                            Map<String, Object> plate = new HashMap<>();
                            plate.put("id", item.has("id") ? item.get("id").asText() : "");
                            plate.put("name", item.has("name") ? item.get("name").asText() : "");
                            plate.put("description", item.has("description") ? item.get("description").asText() : "");
                            plates.add(plate);
                        }
                    }
                    
                    result.put("success", true);
                    result.put("data", plates);
                    result.put("date", queryDate.toString());
                    result.put("fromCache", false);
                } else {
                    result.put("success", false);
                    result.put("message", "API返回错误");
                }
            }
        } catch (Exception e) {
            log.error("获取板块数据失败", e);
            result.put("success", false);
            result.put("message", "获取板块数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getDongcaiHotThemes(Integer bdType) {
        Map<String, Object> result = new HashMap<>();
        int type = (bdType == null) ? 1 : bdType;

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode args = objectMapper.createObjectNode();
            args.put("bdType", type);
            payload.set("args", args);
            payload.put("client", "android");
            payload.put("randomCode", String.valueOf(System.currentTimeMillis()) + "et375d8f");
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("clientType", "cfw");
            payload.put("clientVersion", "null");

            Request request = createVipmoneyRequestBuilder(DC_VIP_HOT_BD_URL)
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    return result;
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                List<JsonNode> items = pickFirstArray(root,
                        "/data",
                        "/data/list",
                        "/data/data",
                        "/result/data",
                        "/result/list",
                        "/list"
                );

                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode it : items) {
                    if (it == null || it.isNull() || !it.isObject()) continue;
                    Map<String, Object> row = new HashMap<>();
                    row.put("themeCode", asTextOrNull(it, "themeCode"));
                    row.put("themeName", asTextOrNull(it, "themeName"));
                    row.put("newsCode", asTextOrNull(it, "newsCode"));
                    row.put("newsTitle", asTextOrNull(it, "newsTitle"));
                    row.put("newsSummary", asTextOrNull(it, "newsSummary"));
                    row.put("themeLabel", asTextOrNull(it, "themeLabel"));
                    row.put("hotRankScore", asNullableLong(it, "hotRankScore"));
                    list.add(row);
                }

                result.put("success", true);
                result.put("data", list);
                result.put("count", list.size());
                result.put("bdType", type);
                return result;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("bdType", type);
            return result;
        }
    }

    @Override
    public Map<String, Object> getDongcaiHotThemeStocks(String themeCode, Integer pageNum, Integer pageSize, Integer sort, String sortField) {
        Map<String, Object> result = new HashMap<>();
        if (themeCode == null || themeCode.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "themeCode required");
            return result;
        }

        int pn = (pageNum == null || pageNum <= 0) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize <= 0) ? 20 : pageSize;
        int s = sort == null ? -1 : sort;
        String sf = (sortField == null || sortField.trim().isEmpty()) ? "f3" : sortField.trim();

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode args = objectMapper.createObjectNode();
            args.put("themeCode", themeCode.trim());
            args.put("pageSize", ps);
            args.put("pageNum", pn);
            args.put("sort", s);
            args.put("sortField", sf);
            payload.set("args", args);
            payload.put("client", "web");
            payload.put("clientVersion", "8.3");
            payload.put("clientType", "cfw");
            payload.put("randomCode", java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            payload.put("timestamp", System.currentTimeMillis());

            Request request = createVipmoneyRequestBuilder(DC_VIP_THEME_STOCK_LIST_URL)
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    return result;
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode listNode = root.path("data").path("list");
                List<JsonNode> items;
                if (listNode != null && listNode.isArray()) {
                    items = new ArrayList<>();
                    listNode.forEach(items::add);
                } else {
                    items = pickFirstArrayFlexible(root,
                            "/data/list",
                            "/data/data",
                            "/data/result",
                            "/result/data",
                            "/result/list",
                            "/result"
                    );
                }

                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode it : items) {
                    if (it == null || it.isNull()) continue;
                    if (it.isObject()) {
                        Map<String, Object> row = objectMapper.convertValue(it, Map.class);
                        row.put("code", firstNonEmpty(asTextOrNull(it, "securityCode"), asTextOrNull(it, "SECURITY_CODE"), asTextOrNull(it, "SECUCODE"), asTextOrNull(it, "code"), asTextOrNull(it, "stockCode")));
                        row.put("name", firstNonEmpty(asTextOrNull(it, "securityName"), asTextOrNull(it, "SECURITY_NAME"), asTextOrNull(it, "name"), asTextOrNull(it, "stockName")));
                        row.put("raw", it);
                        list.add(row);
                    }
                }

                int persistedCount = persistDcHotThemeStocks(themeCode.trim(), items);

                result.put("success", true);
                result.put("themeCode", themeCode.trim());
                result.put("pageNum", pn);
                result.put("pageSize", ps);
                result.put("sort", s);
                result.put("sortField", sf);
                result.put("data", list);
                result.put("count", list.size());
                result.put("persistedCount", persistedCount);
                return result;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("themeCode", themeCode);
            return result;
        }
    }

    private int persistDcHotThemeStocks(String themeCode, List<JsonNode> items) {
        if (jdbcTemplate == null || items == null || items.isEmpty()) {
            return 0;
        }
        LocalDate dataDate = LocalDate.now();
        try {
            ensureDcHotThemeStockDayTable();

            String sql = "INSERT INTO dc_hot_theme_stock_day (data_date, theme_code, stock_code, stock_name, raw_json, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON CONFLICT (data_date, theme_code, stock_code) DO UPDATE SET stock_name=EXCLUDED.stock_name, raw_json=EXCLUDED.raw_json, updated_at=CURRENT_TIMESTAMP";

            List<Object[]> batch = new ArrayList<>();
            for (JsonNode it : items) {
                if (it == null || it.isNull() || !it.isObject()) continue;
                String code = firstNonEmpty(asTextOrNull(it, "securityCode"), asTextOrNull(it, "SECURITY_CODE"), asTextOrNull(it, "SECUCODE"), asTextOrNull(it, "code"), asTextOrNull(it, "stockCode"));
                if (code == null || code.trim().isEmpty()) continue;
                String name = firstNonEmpty(asTextOrNull(it, "securityName"), asTextOrNull(it, "SECURITY_NAME"), asTextOrNull(it, "name"), asTextOrNull(it, "stockName"));
                String rawJson = objectMapper.writeValueAsString(it);
                batch.add(new Object[]{java.sql.Date.valueOf(dataDate), themeCode, code, name, rawJson});
            }
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
            }
            return batch.size();
        } catch (Exception e) {
            log.warn("persistDcHotThemeStocks failed: {}", e.getMessage());
            return 0;
        }
    }

    private void ensureDcHotThemeStockDayTable() {
        if (dcHotThemeStockDayTableEnsured) {
            return;
        }
        synchronized (this) {
            if (dcHotThemeStockDayTableEnsured) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS dc_hot_theme_stock_day (" +
                            "id SERIAL PRIMARY KEY," +
                            "data_date DATE NOT NULL," +
                            "theme_code VARCHAR(20) NOT NULL," +
                            "stock_code VARCHAR(20) NOT NULL," +
                            "stock_name VARCHAR(100)," +
                            "raw_json TEXT," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "deleted INTEGER DEFAULT 0," +
                            "UNIQUE(data_date, theme_code, stock_code)" +
                            ")"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_dc_hot_theme_stock_day_date ON dc_hot_theme_stock_day (data_date)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_dc_hot_theme_stock_day_theme ON dc_hot_theme_stock_day (data_date, theme_code)");
            dcHotThemeStockDayTableEnsured = true;
        }
    }

    @Override
    public Map<String, Object> getDongcaiHotBoards(Integer boardType, Integer pageNumber, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        int bt = (boardType == null) ? 2 : boardType;
        int pn = (pageNumber == null || pageNumber <= 0) ? 1 : pageNumber;
        int ps = (pageSize == null || pageSize <= 0) ? 100 : pageSize;

        try {
            String filter = "(BOARD_TYPE_NEW=\"" + bt + "\")";
            HttpUrl url = HttpUrl.parse(DC_VIP_DATACENTER_URL).newBuilder()
                    .addQueryParameter("reportName", "RPT_BOARD_BOARDHOTSCORE")
                    .addQueryParameter("columns", "BOARD_CODE,NEW_BOARD_CODE,BOARD_NAME,BOARD_TYPE_NEW,HOT_SCORE,HOT_RANK_CHANGE,HOT_RANK_CHANGERANK,HOT_RANK")
                    .addQueryParameter("filter", filter)
                    .addQueryParameter("source", "SECURITIES")
                    .addQueryParameter("pageNumber", String.valueOf(pn))
                    .addQueryParameter("pageSize", String.valueOf(ps))
                    .addQueryParameter("client", "APP")
                    .addQueryParameter("sortColumns", "HOT_RANK")
                    .addQueryParameter("sortTypes", "1")
                    .addQueryParameter("v", String.valueOf(System.currentTimeMillis()))
                    .build();

            Request request = createVipmoneyRequestBuilder(url.toString()).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    return result;
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                List<JsonNode> items = pickFirstArray(root,
                        "/result/data",
                        "/result/list",
                        "/data",
                        "/data/list"
                );
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode it : items) {
                    if (it == null || it.isNull() || !it.isObject()) continue;
                    Map<String, Object> row = new HashMap<>();
                    row.put("boardCode", firstNonEmpty(asTextOrNull(it, "NEW_BOARD_CODE"), asTextOrNull(it, "BOARD_CODE")));
                    row.put("boardName", asTextOrNull(it, "BOARD_NAME"));
                    row.put("boardType", asTextOrNull(it, "BOARD_TYPE_NEW"));
                    row.put("hotScore", asNullableDouble(it, "HOT_SCORE"));
                    row.put("hotRank", asNullableInt(it, "HOT_RANK"));
                    row.put("hotRankChange", asNullableInt(it, "HOT_RANK_CHANGE"));
                    row.put("hotRankChangeRank", asNullableInt(it, "HOT_RANK_CHANGERANK"));
                    list.add(row);
                }

                result.put("success", true);
                result.put("boardType", bt);
                result.put("pageNumber", pn);
                result.put("pageSize", ps);
                result.put("data", list);
                result.put("count", list.size());
                return result;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("boardType", bt);
            return result;
        }
    }

    @Override
    public Map<String, Object> getDongcaiHotBoardStocks(String boardCode, Integer pn, Integer pz) {
        Map<String, Object> result = new HashMap<>();
        if (boardCode == null || boardCode.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "boardCode required");
            return result;
        }

        String bc = boardCode.trim();
        int pageNo = (pn == null || pn <= 0) ? 1 : pn;
        int pageSize = (pz == null || pz <= 0) ? 10 : pz;

        try {
            HttpUrl url = HttpUrl.parse(DC_PUSH2_BOARD_STOCKS_URL).newBuilder()
                    .addQueryParameter("ut", DC_PUSH2_UT)
                    .addQueryParameter("forcect", "1")
                    .addQueryParameter("fs", "b:" + bc)
                    .addQueryParameter("pn", String.valueOf(pageNo))
                    .addQueryParameter("pz", String.valueOf(pageSize))
                    .addQueryParameter("po", "1")
                    .addQueryParameter("fid", "f2")
                    .addQueryParameter("invt", "2")
                    .addQueryParameter("mpi", "1000")
                    .addQueryParameter("fields", "f12,f14,f2,f3,f5,f6,f7,f8,f20,f21,f62,f66,f69,f184")
                    .build();

            Request request = createEmwapRequestBuilder(url.toString(), bc).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    return result;
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode diff = root.path("data").path("diff");
                List<Map<String, Object>> list = new ArrayList<>();

                if (diff.isArray()) {
                    for (JsonNode it : diff) {
                        if (it == null || it.isNull() || !it.isObject()) continue;
                        Map<String, Object> row = new HashMap<>();
                        row.put("code", asTextOrNull(it, "f12"));
                        row.put("name", asTextOrNull(it, "f14"));
                        row.put("price", asNullableDouble(it, "f2"));
                        row.put("pct", asNullableDouble(it, "f3"));
                        row.put("vol", asNullableDouble(it, "f5"));
                        row.put("amount", asNullableDouble(it, "f6"));
                        row.put("amp", asNullableDouble(it, "f7"));
                        row.put("turnover", asNullableDouble(it, "f8"));
                        row.put("mv", asNullableDouble(it, "f20"));
                        row.put("floatMv", asNullableDouble(it, "f21"));
                        row.put("mainIn", asNullableDouble(it, "f62"));
                        row.put("raw", it);
                        list.add(row);
                    }
                } else if (diff.isObject()) {
                    java.util.Iterator<java.util.Map.Entry<String, JsonNode>> it = diff.fields();
                    while (it.hasNext()) {
                        java.util.Map.Entry<String, JsonNode> e = it.next();
                        JsonNode n = e.getValue();
                        if (n == null || n.isNull() || !n.isObject()) continue;
                        Map<String, Object> row = new HashMap<>();
                        row.put("code", asTextOrNull(n, "f12"));
                        row.put("name", asTextOrNull(n, "f14"));
                        row.put("price", asNullableDouble(n, "f2"));
                        row.put("pct", asNullableDouble(n, "f3"));
                        row.put("vol", asNullableDouble(n, "f5"));
                        row.put("amount", asNullableDouble(n, "f6"));
                        row.put("amp", asNullableDouble(n, "f7"));
                        row.put("turnover", asNullableDouble(n, "f8"));
                        row.put("mv", asNullableDouble(n, "f20"));
                        row.put("floatMv", asNullableDouble(n, "f21"));
                        row.put("mainIn", asNullableDouble(n, "f62"));
                        row.put("raw", n);
                        list.add(row);
                    }
                }

                result.put("success", true);
                result.put("boardCode", bc);
                result.put("pn", pageNo);
                result.put("pz", pageSize);
                result.put("data", list);
                result.put("count", list.size());
                return result;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("boardCode", boardCode);
            return result;
        }
    }

    private List<JsonNode> pickFirstArray(JsonNode root, String... pointers) {
        List<JsonNode> empty = new ArrayList<>();
        if (root == null || root.isNull()) return empty;
        if (pointers == null) return empty;
        for (String p : pointers) {
            if (p == null || p.trim().isEmpty()) continue;
            JsonNode n;
            try {
                n = root.at(p);
            } catch (Exception e) {
                n = null;
            }
            if (n != null && n.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                n.forEach(list::add);
                return list;
            }
        }
        if (root.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            root.forEach(list::add);
            return list;
        }
        return empty;
    }

    private List<JsonNode> pickFirstArrayFlexible(JsonNode root, String... pointers) {
        List<JsonNode> items = pickFirstArray(root, pointers);
        if (items != null && !items.isEmpty()) {
            return items;
        }

        if (root == null || root.isNull()) {
            return new ArrayList<>();
        }

        for (String p : pointers) {
            if (p == null || p.trim().isEmpty()) continue;
            JsonNode n;
            try {
                n = root.at(p);
            } catch (Exception e) {
                n = null;
            }
            if (n == null || n.isNull()) continue;

            if (n.isObject()) {
                List<JsonNode> sub = pickFirstArrayFromObject(n,
                        "list", "data", "diff", "items", "result", "rows", "stockList", "stocks");
                if (!sub.isEmpty()) return sub;
            }
        }

        JsonNode found = findFirstArrayNode(root, 0, 6);
        if (found != null && found.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            found.forEach(list::add);
            return list;
        }
        return new ArrayList<>();
    }

    private List<JsonNode> pickFirstArrayFromObject(JsonNode obj, String... keys) {
        List<JsonNode> empty = new ArrayList<>();
        if (obj == null || obj.isNull() || !obj.isObject() || keys == null) return empty;
        for (String k : keys) {
            if (k == null || k.isEmpty()) continue;
            JsonNode n = obj.get(k);
            if (n == null || n.isNull()) continue;
            if (n.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                n.forEach(list::add);
                return list;
            }
            if (n.isObject()) {
                List<JsonNode> deep = pickFirstArrayFromObject(n, keys);
                if (!deep.isEmpty()) return deep;
            }
        }
        return empty;
    }

    private JsonNode findFirstArrayNode(JsonNode node, int depth, int maxDepth) {
        if (node == null || node.isNull()) return null;
        if (depth > maxDepth) return null;
        if (node.isArray()) return node;
        if (node.isObject()) {
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                java.util.Map.Entry<String, JsonNode> e = it.next();
                JsonNode found = findFirstArrayNode(e.getValue(), depth + 1, maxDepth);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String asTextOrNull(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText(null);
        return s == null ? null : s.trim();
    }

    private Integer asNullableInt(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isInt() || v.isLong() || v.isNumber()) return v.asInt();
        try {
            String s = v.asText("").trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Long asNullableLong(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isLong() || v.isInt() || v.isNumber()) return v.asLong();
        try {
            String s = v.asText("").trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double asNullableDouble(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asDouble();
        try {
            String s = v.asText("").trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
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

    @Override
    public Map<String, Object> getStocks(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        LocalDate today = LocalDate.now();
        
        try {
            // 开盘时间：直接从API获取实时数据，避免返回数据库缓存的旧数据
            if (queryDate.equals(today) && isMarketOpen()) {
                log.info("开盘时间从API获取选股通个股数据，日期: {}", queryDate);
                Request request = createRequestBuilder(STOCKS_URL).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        result.put("success", false);
                        result.put("message", "API请求失败");
                        result.put("date", queryDate.toString());
                        return result;
                    }

                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);

                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> stocks = parseStocks(root.get("data"));
                        LocalDate actualDate = resolveActualDateFromStockList(stocks, queryDate);
                        result.put("success", true);
                        result.put("data", stocks);
                        result.put("date", queryDate.toString());
                        result.put("actualDate", actualDate.toString());
                        result.put("fromCache", false);
                        return result;
                    } else {
                        result.put("success", false);
                        result.put("message", "API返回错误");
                        result.put("date", queryDate.toString());
                        return result;
                    }
                }
            }

            // 先从数据库查询（如果数据库可用）
            if (isDbAvailable()) {
                try {
                    List<ThemePlateData> dbData = themePlateDataDao.findByDate(queryDate);
                    if (!dbData.isEmpty()) {
                        log.info("从数据库获取个股数据，日期: {}, 数量: {}", queryDate, dbData.size());
                        List<Map<String, Object>> stocks = dbData.stream().map(this::convertToMap).collect(Collectors.toList());
                        LocalDate actualDate = resolveActualDateFromDbThemePlateData(dbData, queryDate);
                        
                        result.put("success", true);
                        result.put("data", stocks);
                        result.put("date", queryDate.toString());
                        result.put("actualDate", actualDate.toString());
                        result.put("fromCache", true);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("数据库查询失败: {}", e.getMessage());
                    markDbUnavailable();
                }
            }
            
            if (!queryDate.equals(today)) {
                result.put("success", false);
                result.put("message", "历史日期无缓存，请先在当日同步入库");
                result.put("date", queryDate.toString());
                result.put("fromCache", true);
                return result;
            }

            // 数据库无数据，尝试从API获取（选股通个股）
            log.info("数据库无选股通个股数据，尝试从API获取，日期: {}", queryDate);
            
            // 从API获取
            Request request = createRequestBuilder(STOCKS_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                    result.put("date", queryDate.toString());
                    return result;
                }

                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                
                if (root.has("code") && root.get("code").asInt() == 20000) {
                    List<Map<String, Object>> stocks = parseStocks(root.get("data"));
                    LocalDate actualDate = resolveActualDateFromStockList(stocks, queryDate);
                    result.put("success", true);
                    result.put("data", stocks);
                    result.put("date", queryDate.toString());
                    result.put("actualDate", actualDate.toString());
                    result.put("fromCache", false);
                } else {
                    result.put("success", false);
                    result.put("message", "API返回错误");
                }
            }
        } catch (Exception e) {
            log.error("获取个股数据失败", e);
            result.put("success", false);
            result.put("message", "获取个股数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getFullData(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        LocalDate today = LocalDate.now();
        
        try {
            // 非交易时间首次进入：如果数据库还没有当天数据，则先从API拉取并入库，再从数据库返回（确保前端渲染结构一致）
            if (queryDate.equals(today) && !isMarketOpen() && isDbAvailable()) {
                try {
                    int dataCount = themePlateDataDao.countByDate(queryDate);
                    if (dataCount == 0) {
                        log.info("非交易时间且数据库无数据，先从API获取并入库，日期: {}", queryDate);
                        Map<String, Object> apiResult = getFullDataFromApi(queryDate);
                        if (Boolean.TRUE.equals(apiResult.get("success"))) {
                            try {
                                LocalDate actualDate = resolveActualDateFromFullApiResult(apiResult, queryDate);
                                apiResult.put("actualDate", actualDate.toString());
                                if (actualDate.equals(queryDate)) {
                                    saveToDatabase(apiResult, queryDate);
                                    Map<String, Object> dbResult = getFullDataFromDb(queryDate);
                                    dbResult.put("marketStatus", getMarketStatus());
                                    dbResult.put("savedToDb", true);
                                    dbResult.put("actualDate", actualDate.toString());
                                    return dbResult;
                                }

                                saveToDatabase(apiResult, actualDate);
                                apiResult.put("marketStatus", getMarketStatus());
                                apiResult.put("savedToDb", true);
                                apiResult.put("savedDate", actualDate.toString());
                                return apiResult;
                            } catch (Exception e) {
                                log.warn("入库失败，直接返回API结果: {}", e.getMessage());
                                apiResult.put("marketStatus", getMarketStatus());
                                return apiResult;
                            }
                        }
                        apiResult.put("marketStatus", getMarketStatus());
                        return apiResult;
                    }
                } catch (Exception e) {
                    log.warn("检查数据库缓存失败，继续按原逻辑处理: {}", e.getMessage());
                }
            }

            // 判断是否应该从API获取
            boolean fetchFromApi = shouldFetchFromApi(queryDate);
            
            // 如果不需要从API获取，先尝试从数据库读取
            if (!fetchFromApi && isDbAvailable()) {
                try {
                    int dataCount = themePlateDataDao.countByDate(queryDate);
                    if (dataCount > 0) {
                        log.info("从数据库获取完整数据，日期: {}, 数量: {}", queryDate, dataCount);
                        Map<String, Object> dbResult = getFullDataFromDb(queryDate);
                        dbResult.put("marketStatus", getMarketStatus());
                        return dbResult;
                    }
                } catch (Exception e) {
                    log.warn("数据库查询失败，尝试从API获取: {}", e.getMessage());
                    markDbUnavailable();
                    fetchFromApi = queryDate.equals(today);
                }
            }
            
            // 查询的不是今天且数据库没有数据
            if (!queryDate.equals(today)) {
                result.put("success", false);
                result.put("message", "历史日期无缓存，请先在当日同步入库");
                result.put("date", queryDate.toString());
                result.put("marketStatus", getMarketStatus());
                return result;
            }
            
            // 从API获取数据
            log.info("从API获取数据，日期: {}", queryDate);
            result = getFullDataFromApi(queryDate);
            result.put("marketStatus", getMarketStatus());
            
            // 非交易时间或缓存缺失时保存到数据库
            if (Boolean.TRUE.equals(result.get("success")) && isDbAvailable()) {
                try {
                    boolean shouldSave;
                    try {
                        shouldSave = !isMarketOpen() || themePlateDataDao.countByDate(queryDate) == 0;
                    } catch (Exception e) {
                        shouldSave = !isMarketOpen();
                    }

                    if (shouldSave) {
                        LocalDate actualDate = resolveActualDateFromFullApiResult(result, queryDate);
                        result.put("actualDate", actualDate.toString());
                        saveToDatabase(result, actualDate);
                        result.put("savedToDb", true);
                        result.put("savedDate", actualDate.toString());
                    }
                } catch (Exception e) {
                    log.warn("保存数据到数据库失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("获取完整题材板块数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        result.put("date", queryDate.toString());
        return result;
    }

    @Override
    public Map<String, Object> syncWuyangData(String date) {
        return syncWuyangData(date, false);
    }

    @Override
    public Map<String, Object> syncWuyangData(String date, boolean force) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate;

        try {
            queryDate = resolveWuyangDate(date);
            result.put("date", queryDate.toString());

            if (!force && isDbAvailable()) {
                try {
                    int subjectCount = wuyangThemeSubjectDao.countDistinctSubjectsByDate(queryDate);
                    int stockCount = wuyangThemeSubjectDao.countStocksByDate(queryDate);
                    if (subjectCount > 0 && stockCount > 0) {
                        result.put("success", true);
                        result.put("message", "数据库已有数据，无需同步");
                        result.put("date", queryDate.toString());
                        result.put("subjectCount", subjectCount);
                        result.put("stockCount", stockCount);
                        result.put("synced", false);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("舞阳题材数据库查询失败: {}", e.getMessage());
                }
            }

            if (force && isDbAvailable()) {
                try {
                    wuyangThemeSubjectDao.physicalDeleteByDate(queryDate);
                } catch (Exception e) {
                    log.warn("清理舞阳题材旧数据失败: {}", e.getMessage());
                }
            }

            log.info("开始从API同步舞阳题材数据，日期: {}", queryDate);
            Map<String, Object> api = getWuyangDataFromApi(queryDate);
            if (!Boolean.TRUE.equals(api.get("success"))) {
                result.put("success", false);
                result.put("message", "从API获取舞阳题材数据失败: " + api.get("message"));
                result.put("url", api.get("url"));
                result.put("statusCode", api.get("statusCode"));
                result.put("statusMessage", api.get("statusMessage"));
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subjects = (List<Map<String, Object>>) api.get("subjects");
            int totalStocks = ((Number) api.getOrDefault("totalStocks", 0)).intValue();

            if (isDbAvailable()) {
                saveWuyangToDb(queryDate, subjects, force);
            }

            result.put("success", true);
            result.put("message", "舞阳题材数据同步成功");
            result.put("subjectCount", subjects != null ? subjects.size() : 0);
            result.put("stockCount", totalStocks);
            result.put("synced", true);
        } catch (Exception e) {
            log.error("同步舞阳题材数据失败", e);
            result.put("success", false);
            result.put("message", "同步舞阳题材数据失败: " + e.getMessage());
        }

        if (!result.containsKey("date")) {
            result.put("date", LocalDate.now().toString());
        }
        return result;
    }

    @Override
    public Map<String, Object> getWuyangSubjects(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate;
        try {
            queryDate = resolveWuyangDate(date);
            result.put("date", queryDate.toString());
            if (isDbAvailable()) {
                List<WuyangThemeSubject> db = wuyangThemeSubjectDao.findSubjectsWithZtCountByDate(queryDate);
                if (db != null && !db.isEmpty()) {
                    List<Map<String, Object>> subjects = new ArrayList<>();
                    for (WuyangThemeSubject s : db) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", s.getSubjectName());
                        m.put("detail", s.getSubjectDetail());
                        m.put("ztCount", s.getZtCount() != null ? s.getZtCount() : 0);
                        subjects.add(m);
                    }
                    result.put("success", true);
                    result.put("data", subjects);
                    result.put("fromCache", true);
                    return result;
                }
            }

            Map<String, Object> sync = syncWuyangData(queryDate.toString(), false);
            if (!Boolean.TRUE.equals(sync.get("success"))) {
                result.put("success", false);
                result.put("message", String.valueOf(sync.get("message")));
                return result;
            }

            if (isDbAvailable()) {
                List<WuyangThemeSubject> db = wuyangThemeSubjectDao.findSubjectsWithZtCountByDate(queryDate);
                List<Map<String, Object>> subjects = new ArrayList<>();
                if (db != null) {
                    for (WuyangThemeSubject s : db) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", s.getSubjectName());
                        m.put("detail", s.getSubjectDetail());
                        m.put("ztCount", s.getZtCount() != null ? s.getZtCount() : 0);
                        subjects.add(m);
                    }
                }
                result.put("success", true);
                result.put("data", subjects);
                result.put("fromCache", true);
                return result;
            }

            result.put("success", false);
            result.put("message", "数据库不可用");
        } catch (Exception e) {
            log.error("获取舞阳题材列表失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        if (!result.containsKey("date")) {
            result.put("date", LocalDate.now().toString());
        }
        return result;
    }

    @Override
    public Map<String, Object> getWuyangStocks(String date, String subjectName) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate;
        try {
            queryDate = resolveWuyangDate(date);
            result.put("date", queryDate.toString());
            if (subjectName == null || subjectName.isEmpty()) {
                result.put("success", false);
                result.put("message", "subjectName不能为空");
                return result;
            }

            if (isDbAvailable()) {
                List<WuyangThemeSubject> db = wuyangThemeSubjectDao.findStocksByDateAndSubjectName(queryDate, subjectName);
                if (db != null && !db.isEmpty()) {
                    List<Map<String, Object>> stocks = new ArrayList<>();
                    for (WuyangThemeSubject s : db) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("code", s.getStockCode());
                        m.put("name", s.getStockName());
                        m.put("lbCount", s.getLbCount());
                        m.put("firstZtTime", s.getFirstZtTime());
                        m.put("lastZtTime", s.getLastZtTime());
                        m.put("price", s.getPrice());
                        m.put("percent", s.getPercent());
                        m.put("amount", s.getAmount());
                        m.put("reason", s.getReason());
                        m.put("isZt", s.getIsZt());
                        stocks.add(m);
                    }
                    result.put("success", true);
                    result.put("data", stocks);
                    result.put("subjectName", subjectName);
                    result.put("fromCache", true);
                    return result;
                }
            }

            Map<String, Object> sync = syncWuyangData(queryDate.toString(), false);
            if (!Boolean.TRUE.equals(sync.get("success"))) {
                result.put("success", false);
                result.put("message", String.valueOf(sync.get("message")));
                result.put("date", queryDate.toString());
                return result;
            }

            if (isDbAvailable()) {
                List<WuyangThemeSubject> db = wuyangThemeSubjectDao.findStocksByDateAndSubjectName(queryDate, subjectName);
                List<Map<String, Object>> stocks = new ArrayList<>();
                if (db != null) {
                    for (WuyangThemeSubject s : db) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("code", s.getStockCode());
                        m.put("name", s.getStockName());
                        m.put("lbCount", s.getLbCount());
                        m.put("firstZtTime", s.getFirstZtTime());
                        m.put("lastZtTime", s.getLastZtTime());
                        m.put("price", s.getPrice());
                        m.put("percent", s.getPercent());
                        m.put("amount", s.getAmount());
                        m.put("reason", s.getReason());
                        m.put("isZt", s.getIsZt());
                        stocks.add(m);
                    }
                }
                result.put("success", true);
                result.put("data", stocks);
                result.put("subjectName", subjectName);
                result.put("fromCache", true);
                return result;
            }

            result.put("success", false);
            result.put("message", "数据库不可用");
        } catch (Exception e) {
            log.error("获取舞阳题材股票失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        if (!result.containsKey("date")) {
            result.put("date", LocalDate.now().toString());
        }
        result.put("subjectName", subjectName);
        return result;
    }

    private Map<String, Object> getWuyangDataFromApi(LocalDate queryDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            String dateStr = queryDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = String.format(WUYANG_REPLAYROBOT_JSON_URL_TEMPLATE, dateStr);
            result.put("url", url);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("accept", "application/json, text/plain, */*")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("referer", "https://www.wuylh.com/replayrobot/index.html")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.put("success", false);
                    result.put("statusCode", response.code());
                    result.put("statusMessage", response.message());
                    result.put("message", "API请求失败");
                    return result;
                }

                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);

                List<Map<String, Object>> subjects = new ArrayList<>();
                Set<String> coveredCodes = new HashSet<>();

                JsonNode sacs = root.get("sacs");
                if (sacs != null && sacs.isArray()) {
                    for (JsonNode s : sacs) {
                        Map<String, Object> subject = new HashMap<>();
                        String subjectName = s.has("sac") ? s.get("sac").asText() : "";
                        String detail = s.has("detail") ? s.get("detail").asText() : "";
                        subject.put("name", subjectName);
                        subject.put("detail", detail);

                        List<Map<String, Object>> stocks = new ArrayList<>();
                        JsonNode datas = s.get("datas");
                        if (datas != null && datas.isArray()) {
                            for (JsonNode item : datas) {
                                Map<String, Object> stock = parseWuyangStock(item);
                                if (stock != null) {
                                    Object codeObj = stock.getOrDefault("code", "");
                                    if (codeObj != null) {
                                        coveredCodes.add(String.valueOf(codeObj));
                                    }
                                    stocks.add(stock);
                                }
                            }
                        }
                        subject.put("stocks", stocks);
                        subjects.add(subject);
                    }
                }

                List<Map<String, Object>> otherStocks = new ArrayList<>();
                otherStocks.addAll(collectOtherWuyangStocks(root, "lbg", coveredCodes));
                otherStocks.addAll(collectOtherWuyangStocks(root, "qt", coveredCodes));
                otherStocks.addAll(collectOtherWuyangStocks(root, "gg", coveredCodes));
                if (!otherStocks.isEmpty()) {
                    Map<String, Object> other = new HashMap<>();
                    other.put("name", "其他");
                    other.put("detail", "未归类的涨停和破板股票");
                    other.put("stocks", otherStocks);
                    subjects.add(other);
                }

                int totalStocks = 0;
                for (Map<String, Object> s : subjects) {
                    Object stocksObj = s.get("stocks");
                    if (stocksObj instanceof List) {
                        totalStocks += ((List<?>) stocksObj).size();
                    }
                }

                result.put("success", true);
                result.put("date", queryDate.toString());
                result.put("subjects", subjects);
                result.put("totalStocks", totalStocks);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        return result;
    }

    private LocalDate resolveWuyangDate(String dateParam) {
        String s = (dateParam == null) ? "" : dateParam.trim();
        LocalDate parsed = parseDate(s);
        if (parsed.equals(LocalDate.now())) {
            String trade = fetchWuyangTradeDate();
            if (trade != null && !trade.trim().isEmpty()) {
                String t = trade.trim().replace("-", "");
                if (t.length() == 8) {
                    return LocalDate.parse(t, DateTimeFormatter.BASIC_ISO_DATE);
                }
            }
        }

        String normalized = s.replace("-", "");
        if (normalized.length() == 8) {
            return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
        }

        return parsed;
    }

    private String fetchWuyangTradeDate() {
        try {
            Request request = new Request.Builder()
                    .url(WUYANG_RECENTLY_TRADE_DATE_URL)
                    .addHeader("accept", "application/json, text/plain, */*")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }
                String body = response.body().string();
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.get("data");
                if (data == null || data.isNull()) {
                    return null;
                }
                String v = data.asText();
                if (v == null) {
                    return null;
                }
                return v.replace("-", "");
            }
        } catch (Exception e) {
            log.warn("获取舞阳最近交易日失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseWuyangStock(JsonNode item) {
        if (item == null || item.isNull() || !item.isObject()) return null;

        Map<String, Object> stock = new HashMap<>();
        stock.put("code", item.has("code") ? item.get("code").asText() : "");
        stock.put("name", item.has("name") ? item.get("name").asText() : "");
        stock.put("lb_count", item.has("lb_count") ? item.get("lb_count").asText() : "");
        stock.put("zt_time", item.has("zt_time") ? item.get("zt_time").asText() : "");
        stock.put("last_zttime", item.has("last_zttime") ? item.get("last_zttime").asText() : "");
        stock.put("price", item.has("price") ? item.get("price").asText() : "");
        stock.put("precent", item.has("precent") ? item.get("precent").asText() : "");
        stock.put("amount", item.has("amount") ? item.get("amount").asText() : "");
        stock.put("subject_deatal", item.has("subject_deatal") ? item.get("subject_deatal").asText() : "");
        stock.put("fb_detail", item.has("fb_detail") ? item.get("fb_detail").asText() : "");
        stock.put("color", item.has("color") ? item.get("color").asText() : "");
        return stock;
    }

    private List<Map<String, Object>> collectOtherWuyangStocks(JsonNode root, String key, Set<String> coveredCodes) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (root == null) return out;

        JsonNode node = root.get(key);
        if (node == null || !node.isObject()) return out;
        JsonNode datas = node.get("datas");
        if (datas == null || !datas.isArray()) return out;

        for (JsonNode item : datas) {
            if (item == null || !item.isObject()) continue;
            String color = item.has("color") ? item.get("color").asText() : "";
            if (!"red".equalsIgnoreCase(color) && !"green".equalsIgnoreCase(color)) continue;

            Map<String, Object> stock = parseWuyangStock(item);
            if (stock == null) continue;
            String code = String.valueOf(stock.getOrDefault("code", ""));
            if (code.isEmpty() || coveredCodes.contains(code)) continue;
            coveredCodes.add(code);
            out.add(stock);
        }

        return out;
    }

    private void saveWuyangToDb(LocalDate queryDate, List<Map<String, Object>> subjects, boolean alreadyCleared) {
        if (!isDbAvailable()) return;
        if (subjects == null) return;

        try {
            if (!alreadyCleared) {
                wuyangThemeSubjectDao.physicalDeleteByDate(queryDate);
            }

            for (Map<String, Object> subject : subjects) {
                String subjectName = String.valueOf(subject.getOrDefault("name", ""));
                String subjectDetail = String.valueOf(subject.getOrDefault("detail", ""));
                Object stocksObj = subject.get("stocks");
                if (!(stocksObj instanceof List)) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stocks = (List<Map<String, Object>>) stocksObj;
                for (Map<String, Object> s : stocks) {
                    String code = String.valueOf(s.getOrDefault("code", ""));
                    if (code == null) code = "";
                    String name = String.valueOf(s.getOrDefault("name", ""));

                    WuyangThemeSubject entity = new WuyangThemeSubject();
                    entity.setDataDate(queryDate);
                    entity.setSubjectName(subjectName);
                    entity.setSubjectDetail(subjectDetail);
                    entity.setStockCode(code);
                    entity.setStockName(name);
                    entity.setLbCount(String.valueOf(s.getOrDefault("lb_count", "")));
                    entity.setFirstZtTime(String.valueOf(s.getOrDefault("zt_time", "")));
                    entity.setLastZtTime(String.valueOf(s.getOrDefault("last_zttime", "")));
                    entity.setReason(String.valueOf(s.getOrDefault("subject_deatal", "")));

                    String fb = String.valueOf(s.getOrDefault("fb_detail", ""));
                    entity.setIsZt("封板".equals(fb));

                    entity.setPrice(parseBigDecimal(String.valueOf(s.getOrDefault("price", ""))));
                    entity.setPercent(parseBigDecimal(String.valueOf(s.getOrDefault("precent", ""))));
                    entity.setAmount(parseBigDecimal(String.valueOf(s.getOrDefault("amount", ""))));

                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    entity.setDeleted(0);

                    try {
                        wuyangThemeSubjectDao.insert(entity);
                    } catch (Exception e) {
                        log.debug("插入舞阳题材数据失败（可能重复）: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("保存舞阳题材数据到数据库失败: {}", e.getMessage());
        }
    }

    private BigDecimal parseBigDecimal(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getDongcaiStocks(String date, Long plateId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        
        try {
            // 先从数据库查询
            if (isDbAvailable()) {
                try {
                    List<DongcaiThemePlate> dbData = dongcaiThemePlateDao.findStocksByDateAndPlateId(queryDate, plateId);
                    if (!dbData.isEmpty()) {
                        log.info("从数据库获取东财股票数据，日期: {}, 板块ID: {}, 数量: {}", queryDate, plateId, dbData.size());
                        List<Map<String, Object>> stocks = dbData.stream().map(s -> {
                            Map<String, Object> stock = new HashMap<>();
                            stock.put("code", s.getStockCode());
                            stock.put("name", s.getStockName());
                            stock.put("changeRate", s.getChangeRate() != null ? (s.getChangeRate() * 100) : 0);
                            stock.put("description", s.getDescription());
                            stock.put("lianban", s.getLianban());
                            return stock;
                        }).collect(Collectors.toList());
                        
                        result.put("success", true);
                        result.put("data", stocks);
                        result.put("date", queryDate.toString());
                        result.put("plateId", plateId);
                        result.put("fromCache", true);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("数据库查询失败: {}", e.getMessage());
                    markDbUnavailable();
                }
            }
            
            // 数据库无数据，尝试从API获取（东财股票）
            log.info("数据库无东财股票数据，尝试从API获取，日期: {}, 板块ID: {}", queryDate, plateId);
            
            // 从API获取
            Request request = createRequestBuilder(DONGCAI_STOCKS_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    
                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> stocks = new ArrayList<>();
                        JsonNode data = root.get("data");
                        JsonNode items = data.has("items") ? data.get("items") : data;
                        
                        if (items.isArray()) {
                            for (JsonNode itemNode : items) {
                                if (!itemNode.isArray() || itemNode.size() < 12) continue;
                                
                                JsonNode plates = itemNode.get(8);
                                if (!plates.isArray()) continue;
                                
                                // 检查是否属于目标板块
                                boolean belongsToPlate = false;
                                for (JsonNode plate : plates) {
                                    if (plate.has("id") && plate.get("id").asLong() == plateId) {
                                        belongsToPlate = true;
                                        break;
                                    }
                                }
                                
                                if (!belongsToPlate) continue;
                                
                                Map<String, Object> stock = new HashMap<>();
                                stock.put("code", itemNode.get(0).asText().replace(".SZ", "").replace(".SS", "").replace(".SH", ""));
                                stock.put("name", itemNode.get(1).asText());
                                stock.put("changeRate", itemNode.get(3).asDouble() * 100);
                                stock.put("description", itemNode.get(5).asText());
                                stock.put("lianban", itemNode.get(11).asText());
                                stocks.add(stock);
                            }
                        }
                        
                        // 存储到数据库
                        saveDongcaiStocksToDb(queryDate, plateId, stocks);
                        
                        result.put("success", true);
                        result.put("data", stocks);
                        result.put("date", queryDate.toString());
                        result.put("plateId", plateId);
                        result.put("fromCache", false);
                    } else {
                        result.put("success", false);
                        result.put("message", "API返回错误");
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                }
            }
        } catch (Exception e) {
            log.error("获取东财股票数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        result.put("date", queryDate.toString());
        result.put("plateId", plateId);
        return result;
    }

    private void physicalDeleteByDate(LocalDate queryDate) {
        try {
            LambdaQueryWrapper<ThemePlateData> dataWrapper = new LambdaQueryWrapper<>();
            dataWrapper.eq(ThemePlateData::getDataDate, queryDate);
            themePlateDataDao.delete(dataWrapper);

            LambdaQueryWrapper<ThemePlateSummary> summaryWrapper = new LambdaQueryWrapper<>();
            summaryWrapper.eq(ThemePlateSummary::getDataDate, queryDate);
            themePlateSummaryDao.delete(summaryWrapper);
        } catch (Exception e) {
            log.warn("清理旧数据失败，date={}, msg={}", queryDate, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDongcaiFullData(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        
        try {
            // 获取板块数据
            Map<String, Object> platesResult = getDongcaiPlates(date);
            if (!(boolean) platesResult.get("success")) {
                return platesResult;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> plates = (List<Map<String, Object>>) platesResult.get("data");
            
            // 获取每个板块的股票数据
            List<Map<String, Object>> groups = new ArrayList<>();
            for (Map<String, Object> plate : plates) {
                Long plateId = ((Number) plate.get("id")).longValue();
                Map<String, Object> stocksResult = getDongcaiStocks(date, plateId);
                
                Map<String, Object> group = new HashMap<>();
                group.put("plateId", plateId);
                group.put("plateName", plate.get("name"));
                group.put("description", plate.get("description"));
                group.put("count", 0);
                group.put("stocks", new ArrayList<>());
                
                if ((boolean) stocksResult.get("success")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> stocks = (List<Map<String, Object>>) stocksResult.get("data");
                    group.put("count", stocks.size());
                    group.put("stocks", stocks);
                }
                
                groups.add(group);
            }
            
            result.put("success", true);
            Map<String, Object> data = new HashMap<>();
            data.put("groups", groups);
            data.put("totalPlates", plates.size());
            data.put("totalStocks", groups.stream().mapToInt(g -> (Integer) g.get("count")).sum());
            result.put("data", data);
            result.put("date", queryDate.toString());
            result.put("fromCache", platesResult.get("fromCache"));
        } catch (Exception e) {
            log.error("获取完整东财题材数据失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "获取数据失败: " + e.getMessage());
            errorResult.put("date", date);
            return errorResult;
        }
        
        return result;
    }

    private void saveDongcaiPlatesToDb(LocalDate date, List<Map<String, Object>> plates) {
        if (!isDbAvailable()) return;
        
        try {
            for (Map<String, Object> plate : plates) {
                DongcaiThemePlate entity = new DongcaiThemePlate();
                entity.setDataDate(date);
                entity.setPlateId(((Number) plate.get("id")).longValue());
                entity.setPlateName((String) plate.get("name"));
                entity.setPlateDescription((String) plate.get("description"));
                entity.setStockCode(""); // 板块记录不需要股票代码
                entity.setStockName(""); // 板块记录不需要股票名称
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setDeleted(0);
                
                dongcaiThemePlateDao.insert(entity);
            }
            log.info("保存东财板块数据到数据库，日期: {}, 数量: {}", date, plates.size());
        } catch (Exception e) {
            log.warn("保存东财板块数据到数据库失败: {}", e.getMessage());
        }
    }

    private void saveDongcaiStocksToDb(LocalDate date, Long plateId, List<Map<String, Object>> stocks) {
        if (!isDbAvailable()) return;
        
        try {
            // 先获取板块信息
            String plateName = "";
            String plateDesc = "";
            try {
                List<DongcaiThemePlate> plates = dongcaiThemePlateDao.findPlatesByDate(date);
                for (DongcaiThemePlate p : plates) {
                    if (p != null && p.getPlateId() != null && p.getPlateId().equals(plateId)) {
                        plateName = p.getPlateName();
                        plateDesc = p.getPlateDescription();
                        break;
                    }
                }
            } catch (Exception ignore) {
                // ignore
            }
            
            for (Map<String, Object> stock : stocks) {
                DongcaiThemePlate entity = new DongcaiThemePlate();
                entity.setDataDate(date);
                entity.setPlateId(plateId);
                entity.setPlateName(plateName);
                entity.setPlateDescription(plateDesc);
                entity.setStockCode((String) stock.get("code"));
                entity.setStockName((String) stock.get("name"));
                entity.setChangeRate(((Number) stock.get("changeRate")).doubleValue() / 100);
                entity.setDescription((String) stock.get("description"));
                entity.setLianban((String) stock.get("lianban"));
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setDeleted(0);
                
                dongcaiThemePlateDao.insert(entity);
            }
            log.info("保存东财股票数据到数据库，日期: {}, 板块ID: {}, 数量: {}", date, plateId, stocks.size());
        } catch (Exception e) {
            log.warn("保存东财股票数据到数据库失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDongcaiPlates(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        
        try {
            // 先从数据库查询
            if (isDbAvailable()) {
                try {
                    List<DongcaiThemePlate> dbData = dongcaiThemePlateDao.findPlatesWithZtCountByDate(queryDate);
                    if (!dbData.isEmpty()) {
                        log.info("从数据库获取东财板块数据，日期: {}, 数量: {}", queryDate, dbData.size());
                        List<Map<String, Object>> plates = dbData.stream().map(p -> {
                            Map<String, Object> plate = new HashMap<>();
                            plate.put("id", p.getPlateId());
                            plate.put("name", p.getPlateName());
                            plate.put("description", p.getPlateDescription());
                            plate.put("ztCount", p.getZtCount() != null ? p.getZtCount() : 0);
                            return plate;
                        }).collect(Collectors.toList());
                        
                        result.put("success", true);
                        result.put("data", plates);
                        result.put("date", queryDate.toString());
                        result.put("fromCache", true);
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("数据库查询失败: {}", e.getMessage());
                    markDbUnavailable();
                }
            }
            
            // 数据库无数据，尝试从API获取（东财板块）
            log.info("数据库无东财板块数据，尝试从API获取，日期: {}", queryDate);
            
            // 从API获取
            Request request = createRequestBuilder(DONGCAI_PLATES_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    
                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> plates = new ArrayList<>();
                        JsonNode data = root.get("data");
                        JsonNode items = data.has("items") ? data.get("items") : data;
                        
                        if (items.isArray()) {
                            for (JsonNode item : items) {
                                Map<String, Object> plate = new HashMap<>();
                                plate.put("id", item.has("id") ? item.get("id").asLong() : 0L);
                                plate.put("name", item.has("name") ? item.get("name").asText() : "");
                                plate.put("description", item.has("description") ? item.get("description").asText() : "");
                                plates.add(plate);
                            }
                        }
                        
                        // 存储到数据库
                        saveDongcaiPlatesToDb(queryDate, plates);
                        
                        result.put("success", true);
                        result.put("data", plates);
                        result.put("date", queryDate.toString());
                        result.put("fromCache", false);
                    } else {
                        result.put("success", false);
                        result.put("message", "API返回错误");
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "API请求失败");
                }
            }
        } catch (Exception e) {
            log.error("获取东财板块数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        result.put("date", queryDate.toString());
        return result;
    }
    
    /**
     * 获取当前市场状态
     */
    private String getMarketStatus() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (!isTradingDay(today)) {
            return "休市";
        }
        
        if (now.isBefore(MARKET_OPEN)) {
            return "盘前";
        } else if (now.isAfter(MARKET_CLOSE)) {
            return "收盘";
        } else {
            return "交易中";
        }
    }
    
    /**
     * 从数据库获取完整数据
     */
    private Map<String, Object> getFullDataFromDb(LocalDate queryDate) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取板块汇总
        List<ThemePlateSummary> summaries = themePlateSummaryDao.findByDate(queryDate);
        List<Map<String, Object>> plates = summaries.stream().map(s -> {
            Map<String, Object> plate = new HashMap<>();
            plate.put("id", s.getPlateId());
            plate.put("name", s.getPlateName());
            plate.put("description", s.getPlateDescription());
            return plate;
        }).collect(Collectors.toList());
        
        // 获取个股数据并按板块分组
        List<ThemePlateData> allData = themePlateDataDao.findByDate(queryDate);
        LocalDate actualDate = resolveActualDateFromDbThemePlateData(allData, queryDate);
        Map<String, List<ThemePlateData>> groupedData = allData.stream()
                .collect(Collectors.groupingBy(ThemePlateData::getPlateName, LinkedHashMap::new, Collectors.toList()));
        
        // 构建分组结果
        List<Map<String, Object>> groups = new ArrayList<>();
        
        // 按板块顺序排列
        for (ThemePlateSummary summary : summaries) {
            String plateName = summary.getPlateName();
            List<ThemePlateData> plateStocks = groupedData.getOrDefault(plateName, new ArrayList<>());
            
            Map<String, Object> group = new HashMap<>();
            group.put("plateName", plateName);
            group.put("description", summary.getPlateDescription());
            group.put("count", plateStocks.size());
            group.put("stocks", plateStocks.stream()
                    .sorted((a, b) -> {
                        // 有连板的排前面
                        int boardsA = extractBoards(a.getMDaysNBoards());
                        int boardsB = extractBoards(b.getMDaysNBoards());
                        if (boardsA > 0 && boardsB == 0) return -1;
                        if (boardsA == 0 && boardsB > 0) return 1;
                        if (boardsA != boardsB) return boardsB - boardsA;
                        // 按涨停时间排序
                        long timeA = a.getEnterTime() != null ? a.getEnterTime() : 0L;
                        long timeB = b.getEnterTime() != null ? b.getEnterTime() : 0L;
                        return Long.compare(timeA, timeB);
                    })
                    .map(this::convertToMap)
                    .collect(Collectors.toList()));
            groups.add(group);
        }
        
        // 添加不在汇总中的板块
        Set<String> addedPlates = summaries.stream().map(ThemePlateSummary::getPlateName).collect(Collectors.toSet());
        for (String plateName : groupedData.keySet()) {
            if (!addedPlates.contains(plateName)) {
                List<ThemePlateData> plateStocks = groupedData.get(plateName);
                Map<String, Object> group = new HashMap<>();
                group.put("plateName", plateName);
                group.put("description", "");
                group.put("count", plateStocks.size());
                group.put("stocks", plateStocks.stream().map(this::convertToMap).collect(Collectors.toList()));
                groups.add(group);
            }
        }
        
        result.put("success", true);
        result.put("plates", plates);
        result.put("groups", groups);
        result.put("totalStocks", allData.size());
        result.put("date", queryDate.toString());
        result.put("actualDate", actualDate.toString());
        result.put("fromCache", true);
        
        return result;
    }
    
    /**
     * 从API获取完整数据
     */
    private Map<String, Object> getFullDataFromApi(LocalDate queryDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取板块数据
            Map<String, Object> platesResult = getPlates(queryDate.toString());
            List<Map<String, Object>> plates = new ArrayList<>();
            List<String> plateOrder = new ArrayList<>();
            
            if (Boolean.TRUE.equals(platesResult.get("success"))) {
                plates = (List<Map<String, Object>>) platesResult.get("data");
                for (Map<String, Object> plate : plates) {
                    plateOrder.add((String) plate.get("name"));
                }
            }

            // 获取个股数据
            Map<String, Object> stocksResult = getStocks(queryDate.toString());
            List<Map<String, Object>> stocks = new ArrayList<>();
            
            if (Boolean.TRUE.equals(stocksResult.get("success"))) {
                stocks = (List<Map<String, Object>>) stocksResult.get("data");
            }

            // 按板块分组
            Map<String, List<Map<String, Object>>> groupedStocks = groupStocksByPlate(stocks);
            
            // 按板块顺序排列
            List<Map<String, Object>> sortedGroups = new ArrayList<>();
            Set<String> addedPlates = new HashSet<>();
            
            for (String plateName : plateOrder) {
                if (groupedStocks.containsKey(plateName)) {
                    Map<String, Object> group = new HashMap<>();
                    group.put("plateName", plateName);
                    group.put("stocks", sortStocksInPlate(groupedStocks.get(plateName)));
                    group.put("count", groupedStocks.get(plateName).size());
                    
                    String desc = "";
                    for (Map<String, Object> plate : plates) {
                        if (plateName.equals(plate.get("name"))) {
                            desc = (String) plate.getOrDefault("description", "");
                            break;
                        }
                    }
                    group.put("description", desc);
                    sortedGroups.add(group);
                    addedPlates.add(plateName);
                }
            }
            
            for (String plateName : groupedStocks.keySet()) {
                if (!addedPlates.contains(plateName)) {
                    Map<String, Object> group = new HashMap<>();
                    group.put("plateName", plateName);
                    group.put("stocks", sortStocksInPlate(groupedStocks.get(plateName)));
                    group.put("count", groupedStocks.get(plateName).size());
                    group.put("description", "");
                    sortedGroups.add(group);
                }
            }

            result.put("success", true);
            result.put("plates", plates);
            result.put("groups", sortedGroups);
            result.put("totalStocks", stocks.size());
            result.put("date", queryDate.toString());
            result.put("fromCache", false);
            
        } catch (Exception e) {
            log.error("从API获取数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> syncData(String date) {
        return syncData(date, false);
    }

    @Override
    public Map<String, Object> syncData(String date, boolean force) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        LocalDate today = LocalDate.now();
        
        try {
            // 只能同步当天数据
            if (!queryDate.equals(today)) {
                result.put("success", false);
                result.put("message", "只能同步当天数据");
                result.put("date", queryDate.toString());
                return result;
            }
            
            if (!force) {
                // 检查数据库是否已有数据
                if (isDbAvailable()) {
                    try {
                        int summaryCount = themePlateSummaryDao.countByDate(queryDate);
                        int dataCount = themePlateDataDao.countByDate(queryDate);
                        if (summaryCount > 0 && dataCount > 0) {
                            LocalDate dbActualDate = queryDate;
                            try {
                                List<ThemePlateData> dbData = themePlateDataDao.findByDate(queryDate);
                                dbActualDate = resolveActualDateFromDbThemePlateData(dbData, queryDate);
                            } catch (Exception ignore) {
                            }

                            if (dbActualDate.equals(queryDate)) {
                                log.info("数据库已有数据，日期: {}, 板块数: {}, 个股数: {}", queryDate, summaryCount, dataCount);
                                result.put("success", true);
                                result.put("message", "数据库已有数据，无需同步");
                                result.put("date", queryDate.toString());
                                result.put("actualDate", dbActualDate.toString());
                                result.put("plateCount", summaryCount);
                                result.put("count", dataCount);
                                result.put("synced", false);
                                return result;
                            }

                            log.info("数据库已有数据但日期不匹配，继续同步。queryDate={}, dbActualDate={}", queryDate, dbActualDate);
                        }
                    } catch (Exception e) {
                        log.warn("数据库查询失败: {}", e.getMessage());
                        markDbUnavailable();
                    }
                }
            }
            
            // 从API获取数据
            log.info("开始从API同步数据，日期: {}", queryDate);
            Map<String, Object> apiResult = getFullDataFromApi(queryDate);
            
            if (!Boolean.TRUE.equals(apiResult.get("success"))) {
                result.put("success", false);
                result.put("message", "从API获取数据失败: " + apiResult.get("message"));
                result.put("date", queryDate.toString());
                return result;
            }

            LocalDate actualDate = resolveActualDateFromFullApiResult(apiResult, queryDate);
            apiResult.put("actualDate", actualDate.toString());
            
            // 保存到数据库
            if (isDbAvailable()) {
                try {
                    if (force) {
                        physicalDeleteByDate(queryDate);
                        if (!actualDate.equals(queryDate)) {
                            physicalDeleteByDate(actualDate);
                        }
                    }

                    saveToDatabase(apiResult, actualDate);
                    log.info("数据同步完成，queryDate: {}, savedDate: {}", queryDate, actualDate);
                    result.put("success", true);
                    if (actualDate.equals(queryDate)) {
                        result.put("message", "数据同步成功");
                    } else {
                        result.put("message", "同步成功（上游返回日期与所选日期不一致）");
                    }
                    result.put("date", queryDate.toString());
                    result.put("actualDate", actualDate.toString());
                    result.put("savedDate", actualDate.toString());
                    result.put("totalStocks", apiResult.get("totalStocks"));
                    result.put("synced", true);
                } catch (Exception e) {
                    log.error("保存数据到数据库失败", e);
                    result.put("success", false);
                    result.put("message", "保存数据到数据库失败: " + e.getMessage());
                }
            } else {
                result.put("success", false);
                result.put("message", "数据库不可用");
            }
            
        } catch (Exception e) {
            log.error("同步数据失败", e);
            result.put("success", false);
            result.put("message", "同步数据失败: " + e.getMessage());
        }
        
        result.put("date", queryDate.toString());
        return result;
    }
    
    @Override
    public Map<String, Object> syncDongcaiData(String date) {
        return syncDongcaiData(date, false);
    }

    @Override
    public Map<String, Object> syncDongcaiData(String date, boolean force) {
        Map<String, Object> result = new HashMap<>();
        LocalDate queryDate = parseDate(date);
        LocalDate today = LocalDate.now();
        
        try {
            // 只能同步当天数据
            if (!queryDate.equals(today)) {
                result.put("success", false);
                result.put("message", "只能同步当天数据");
                result.put("date", queryDate.toString());
                return result;
            }
            
            if (!force) {
                // 检查数据库是否已有数据
                if (isDbAvailable()) {
                    try {
                        int plateCount = dongcaiThemePlateDao.countDistinctPlatesByDate(queryDate);
                        int stockPlateCount = dongcaiThemePlateDao.countDistinctPlatesWithStocksByDate(queryDate);
                        int stockCount = dongcaiThemePlateDao.countByDate(queryDate);
                        if (plateCount > 0 && stockPlateCount >= plateCount && stockCount > 0) {
                            log.info("东财数据库已有数据，日期: {}, 板块数: {}, 已缓存股票板块数: {}, 股票数: {}", queryDate, plateCount, stockPlateCount, stockCount);
                            result.put("success", true);
                            result.put("message", "数据库已有数据，无需同步");
                            result.put("date", queryDate.toString());
                            result.put("plateCount", plateCount);
                            result.put("stockPlateCount", stockPlateCount);
                            result.put("count", stockCount);
                            result.put("synced", false);
                            return result;
                        }
                    } catch (Exception e) {
                        log.warn("数据库查询失败: {}", e.getMessage());
                        markDbUnavailable();
                    }
                }
            }

            if (force && isDbAvailable()) {
                try {
                    dongcaiThemePlateDao.physicalDeleteByDate(queryDate);
                } catch (Exception e) {
                    log.warn("清理东财旧数据失败: {}", e.getMessage());
                }
            }
            
            // 从API获取板块数据
            log.info("开始从API同步东财数据，日期: {}", queryDate);
            Map<String, Object> platesResult = force ? getDongcaiPlatesFromApi(queryDate) : getDongcaiPlates(queryDate.toString());
            
            if (!Boolean.TRUE.equals(platesResult.get("success"))) {
                result.put("success", false);
                result.put("message", "从API获取东财板块数据失败: " + platesResult.get("message"));
                result.put("date", queryDate.toString());
                return result;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> plates = (List<Map<String, Object>>) platesResult.get("data");
            int totalStocks = 0;
            
            // 获取每个板块的股票数据
            for (Map<String, Object> plate : plates) {
                Long plateId = ((Number) plate.get("id")).longValue();
                Map<String, Object> stocksResult = force ? getDongcaiStocksFromApi(queryDate, plateId) : getDongcaiStocks(queryDate.toString(), plateId);
                if (Boolean.TRUE.equals(stocksResult.get("success"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> stocks = (List<Map<String, Object>>) stocksResult.get("data");
                    totalStocks += stocks.size();
                }
            }
            
            log.info("东财数据同步完成，日期: {}, 板块数: {}, 股票数: {}", queryDate, plates.size(), totalStocks);
            result.put("success", true);
            result.put("message", "东财数据同步成功");
            result.put("date", queryDate.toString());
            result.put("totalPlates", plates.size());
            result.put("totalStocks", totalStocks);
            result.put("synced", true);
            
        } catch (Exception e) {
            log.error("同步东财数据失败", e);
            result.put("success", false);
            result.put("message", "同步东财数据失败: " + e.getMessage());
        }
        
        result.put("date", queryDate.toString());
        return result;
    }

    private Map<String, Object> getDongcaiPlatesFromApi(LocalDate queryDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            Request request = createRequestBuilder(DONGCAI_PLATES_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> plates = new ArrayList<>();
                        JsonNode data = root.get("data");
                        JsonNode items = data != null && data.has("items") ? data.get("items") : data;
                        if (items != null && items.isArray()) {
                            for (JsonNode item : items) {
                                Map<String, Object> plate = new HashMap<>();
                                plate.put("id", item.has("id") ? item.get("id").asLong() : 0L);
                                plate.put("name", item.has("name") ? item.get("name").asText() : "");
                                plate.put("description", item.has("description") ? item.get("description").asText() : "");
                                plates.add(plate);
                            }
                        }

                        saveDongcaiPlatesToDb(queryDate, plates);

                        result.put("success", true);
                        result.put("data", plates);
                        result.put("date", queryDate.toString());
                        result.put("fromCache", false);
                        return result;
                    }
                }
            }
            result.put("success", false);
            result.put("message", "API请求失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        result.put("date", queryDate.toString());
        return result;
    }

    private Map<String, Object> getDongcaiStocksFromApi(LocalDate queryDate, Long plateId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Request request = createRequestBuilder(DONGCAI_STOCKS_URL).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    if (root.has("code") && root.get("code").asInt() == 20000) {
                        List<Map<String, Object>> stocks = new ArrayList<>();
                        JsonNode data = root.get("data");
                        JsonNode items = data != null && data.has("items") ? data.get("items") : data;

                        if (items != null && items.isArray()) {
                            for (JsonNode itemNode : items) {
                                if (!itemNode.isArray() || itemNode.size() < 12) continue;

                                JsonNode plates = itemNode.get(8);
                                if (!plates.isArray()) continue;

                                boolean belongsToPlate = false;
                                for (JsonNode plate : plates) {
                                    if (plate.has("id") && plate.get("id").asLong() == plateId) {
                                        belongsToPlate = true;
                                        break;
                                    }
                                }
                                if (!belongsToPlate) continue;

                                Map<String, Object> stock = new HashMap<>();
                                stock.put("code", itemNode.get(0).asText().replace(".SZ", "").replace(".SS", "").replace(".SH", ""));
                                stock.put("name", itemNode.get(1).asText());
                                stock.put("changeRate", itemNode.get(3).asDouble() * 100);
                                stock.put("description", itemNode.get(5).asText());
                                stock.put("lianban", itemNode.get(11).asText());
                                stocks.add(stock);
                            }
                        }

                        saveDongcaiStocksToDb(queryDate, plateId, stocks);

                        result.put("success", true);
                        result.put("data", stocks);
                        result.put("date", queryDate.toString());
                        result.put("plateId", plateId);
                        result.put("fromCache", false);
                        return result;
                    }
                }
            }
            result.put("success", false);
            result.put("message", "API请求失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        result.put("date", queryDate.toString());
        result.put("plateId", plateId);
        return result;
    }
    
    /**
     * 保存数据到数据库
     */
    private void saveToDatabase(Map<String, Object> data, LocalDate queryDate) {
        try {
            // 先删除当天的旧数据
            LambdaQueryWrapper<ThemePlateData> dataWrapper = new LambdaQueryWrapper<>();
            dataWrapper.eq(ThemePlateData::getDataDate, queryDate);
            themePlateDataDao.delete(dataWrapper);
            
            LambdaQueryWrapper<ThemePlateSummary> summaryWrapper = new LambdaQueryWrapper<>();
            summaryWrapper.eq(ThemePlateSummary::getDataDate, queryDate);
            themePlateSummaryDao.delete(summaryWrapper);
            
            // 保存板块汇总
            List<Map<String, Object>> plates = (List<Map<String, Object>>) data.get("plates");
            if (plates != null) {
                for (Map<String, Object> plate : plates) {
                    ThemePlateSummary summary = new ThemePlateSummary();
                    summary.setDataDate(queryDate);
                    summary.setPlateId((String) plate.get("id"));
                    summary.setPlateName((String) plate.get("name"));
                    summary.setPlateDescription((String) plate.get("description"));
                    
                    // 计算该板块的股票数量
                    List<Map<String, Object>> groups = (List<Map<String, Object>>) data.get("groups");
                    int count = 0;
                    if (groups != null) {
                        for (Map<String, Object> group : groups) {
                            if (plate.get("name").equals(group.get("plateName"))) {
                                count = (Integer) group.getOrDefault("count", 0);
                                break;
                            }
                        }
                    }
                    summary.setStockCount(count);
                    
                    themePlateSummaryDao.insert(summary);
                }
            }
            
            // 保存个股数据
            List<Map<String, Object>> groups = (List<Map<String, Object>>) data.get("groups");
            if (groups != null) {
                for (Map<String, Object> group : groups) {
                    String plateName = (String) group.get("plateName");
                    String plateDesc = (String) group.getOrDefault("description", "");
                    List<Map<String, Object>> stocks = (List<Map<String, Object>>) group.get("stocks");
                    
                    if (stocks != null) {
                        for (Map<String, Object> stock : stocks) {
                            ThemePlateData entity = new ThemePlateData();
                            entity.setDataDate(queryDate);
                            entity.setPlateName(plateName);
                            entity.setPlateDescription(plateDesc);
                            entity.setStockCode((String) stock.get("code"));
                            entity.setStockName((String) stock.get("name"));
                            entity.setPrice(toBigDecimal(stock.get("price")));
                            entity.setChangeRate(toBigDecimal(stock.get("changeRate")));
                            entity.setMarketValue(toBigDecimal(stock.get("marketValue")));
                            entity.setTurnoverRatio(toBigDecimal(stock.get("turnoverRatio")));
                            entity.setEnterTime(toLong(stock.get("enterTime")));
                            entity.setIsUpLimit(toBoolean(stock.get("isUpLimit")));
                            entity.setMDaysNBoards((String) stock.get("mDaysNBoards"));
                            entity.setDescription((String) stock.get("description"));
                            
                            try {
                                themePlateDataDao.insert(entity);
                            } catch (Exception e) {
                                // 忽略重复数据错误
                                log.debug("插入数据失败（可能是重复）: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            log.info("数据已保存到数据库，日期: {}", queryDate);
            
        } catch (Exception e) {
            log.error("保存数据到数据库失败", e);
        }
    }

    private LocalDate resolveActualDateFromFullApiResult(Map<String, Object> apiResult, LocalDate fallback) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> groups = (List<Map<String, Object>>) apiResult.get("groups");
            if (groups == null || groups.isEmpty()) {
                return fallback;
            }

            List<Map<String, Object>> stocks = new ArrayList<>();
            for (Map<String, Object> group : groups) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> s = (List<Map<String, Object>>) group.get("stocks");
                if (s != null && !s.isEmpty()) {
                    stocks.addAll(s);
                }
            }
            return resolveActualDateFromStockList(stocks, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDate resolveActualDateFromStockList(List<Map<String, Object>> stocks, LocalDate fallback) {
        if (stocks == null || stocks.isEmpty()) {
            return fallback;
        }

        long max = 0L;
        for (Map<String, Object> stock : stocks) {
            if (stock == null) continue;
            Object v = stock.get("enterTime");
            long t = 0L;
            if (v instanceof Number) {
                t = ((Number) v).longValue();
            } else if (v instanceof String) {
                try {
                    t = Long.parseLong((String) v);
                } catch (Exception ignore) {
                }
            }
            if (t > max) {
                max = t;
            }
        }

        if (max <= 0L) {
            return fallback;
        }

        try {
            return Instant.ofEpochSecond(max).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDate resolveActualDateFromDbThemePlateData(List<ThemePlateData> dbData, LocalDate fallback) {
        if (dbData == null || dbData.isEmpty()) {
            return fallback;
        }

        long max = 0L;
        for (ThemePlateData d : dbData) {
            if (d == null || d.getEnterTime() == null) continue;
            if (d.getEnterTime() > max) {
                max = d.getEnterTime();
            }
        }

        if (max <= 0L) {
            return fallback;
        }

        try {
            return Instant.ofEpochSecond(max).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return fallback;
        }
    }
    
    private Map<String, Object> convertToMap(ThemePlateData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", data.getStockCode());
        map.put("name", data.getStockName());
        map.put("price", data.getPrice() != null ? data.getPrice().doubleValue() : 0.0);
        map.put("changeRate", data.getChangeRate() != null ? data.getChangeRate().doubleValue() : 0.0);
        map.put("marketValue", data.getMarketValue() != null ? data.getMarketValue().doubleValue() : 0.0);
        map.put("turnoverRatio", data.getTurnoverRatio() != null ? data.getTurnoverRatio().doubleValue() : 0.0);
        map.put("enterTime", data.getEnterTime());
        map.put("isUpLimit", data.getIsUpLimit());
        map.put("mDaysNBoards", data.getMDaysNBoards());
        map.put("description", data.getDescription());
        map.put("plates", Collections.singletonList(data.getPlateName()));
        return map;
    }
    
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return null;
    }
    
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }
    
    private Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }
    
    private LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private List<Map<String, Object>> parseStocks(JsonNode data) {
        List<Map<String, Object>> stocks = new ArrayList<>();
        
        JsonNode fields = data.get("fields");
        JsonNode items = data.get("items");
        
        if (fields == null || items == null) return stocks;
        
        Map<String, Integer> fieldIndex = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            fieldIndex.put(fields.get(i).asText(), i);
        }
        
        for (JsonNode item : items) {
            Map<String, Object> stock = new HashMap<>();
            
            stock.put("code", getFieldValue(item, fieldIndex, "code", ""));
            stock.put("name", getFieldValue(item, fieldIndex, "prod_name", ""));
            stock.put("price", getFieldDoubleValue(item, fieldIndex, "cur_price", 0.0));
            stock.put("changeRate", getFieldDoubleValue(item, fieldIndex, "px_change_rate", 0.0));
            stock.put("marketValue", getFieldDoubleValue(item, fieldIndex, "circulation_value", 0.0));
            stock.put("description", getFieldValue(item, fieldIndex, "description", ""));
            stock.put("enterTime", getFieldLongValue(item, fieldIndex, "enter_time", 0L));
            stock.put("isUpLimit", getFieldBoolValue(item, fieldIndex, "up_limit", false));
            stock.put("turnoverRatio", getFieldDoubleValue(item, fieldIndex, "turnover_ratio", 0.0));
            stock.put("mDaysNBoards", getFieldValue(item, fieldIndex, "m_days_n_boards", ""));
            
            List<String> plateNames = new ArrayList<>();
            Integer platesIdx = fieldIndex.get("plates");
            if (platesIdx != null && platesIdx < item.size()) {
                JsonNode platesNode = item.get(platesIdx);
                if (platesNode != null && platesNode.isArray()) {
                    for (JsonNode plateNode : platesNode) {
                        if (plateNode.has("name")) {
                            plateNames.add(plateNode.get("name").asText());
                        }
                    }
                }
            }
            stock.put("plates", plateNames);
            
            stocks.add(stock);
        }
        
        return stocks;
    }

    private String getFieldValue(JsonNode item, Map<String, Integer> fieldIndex, String field, String defaultValue) {
        Integer idx = fieldIndex.get(field);
        if (idx != null && idx < item.size() && !item.get(idx).isNull()) {
            return item.get(idx).asText();
        }
        return defaultValue;
    }

    private double getFieldDoubleValue(JsonNode item, Map<String, Integer> fieldIndex, String field, double defaultValue) {
        Integer idx = fieldIndex.get(field);
        if (idx != null && idx < item.size() && !item.get(idx).isNull()) {
            return item.get(idx).asDouble();
        }
        return defaultValue;
    }

    private long getFieldLongValue(JsonNode item, Map<String, Integer> fieldIndex, String field, long defaultValue) {
        Integer idx = fieldIndex.get(field);
        if (idx != null && idx < item.size() && !item.get(idx).isNull()) {
            return item.get(idx).asLong();
        }
        return defaultValue;
    }

    private boolean getFieldBoolValue(JsonNode item, Map<String, Integer> fieldIndex, String field, boolean defaultValue) {
        Integer idx = fieldIndex.get(field);
        if (idx != null && idx < item.size() && !item.get(idx).isNull()) {
            return item.get(idx).asBoolean();
        }
        return defaultValue;
    }

    private Map<String, List<Map<String, Object>>> groupStocksByPlate(List<Map<String, Object>> stocks) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        
        for (Map<String, Object> stock : stocks) {
            List<String> plates = (List<String>) stock.get("plates");
            if (plates == null || plates.isEmpty()) {
                grouped.computeIfAbsent("其他", k -> new ArrayList<>()).add(stock);
            } else {
                for (String plate : plates) {
                    grouped.computeIfAbsent(plate, k -> new ArrayList<>()).add(stock);
                }
            }
        }
        
        return grouped;
    }

    private List<Map<String, Object>> sortStocksInPlate(List<Map<String, Object>> stocks) {
        stocks.sort((a, b) -> {
            String mDaysA = (String) a.getOrDefault("mDaysNBoards", "");
            String mDaysB = (String) b.getOrDefault("mDaysNBoards", "");
            
            int boardsA = extractBoards(mDaysA);
            int boardsB = extractBoards(mDaysB);
            
            if (boardsA > 0 && boardsB == 0) return -1;
            if (boardsA == 0 && boardsB > 0) return 1;
            if (boardsA != boardsB) return boardsB - boardsA;
            
            long timeA = toLong(a.getOrDefault("enterTime", 0L));
            long timeB = toLong(b.getOrDefault("enterTime", 0L));
            return Long.compare(timeA, timeB);
        });
        
        return stocks;
    }

    private int extractBoards(String mDaysNBoards) {
        if (mDaysNBoards == null || mDaysNBoards.isEmpty()) return 0;
        Pattern pattern = Pattern.compile("(\\d+)天(\\d+)板");
        Matcher matcher = pattern.matcher(mDaysNBoards);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(2));
        }
        return 0;
    }
}
