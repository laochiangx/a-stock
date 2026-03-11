package com.example.stock.service.impl;

import com.example.stock.service.StockDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StockDataServiceImpl implements StockDataService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> getDragonTigerList(String date) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> dragonTigerList = new ArrayList<>();

        try {
            // 如果没有指定日期，先尝试获取最近交易日的数据
            // 龙虎榜数据通常在收盘后1小时左右更新，非交易日没有数据
            String queryDate = date;
            if (queryDate == null || queryDate.isEmpty()) {
                // 先不带日期过滤，获取最新的龙虎榜数据
                queryDate = getLatestTradingDate();
            }

            // EastMoney 接口，与 Go 版 LongTiger 基本一致
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://datacenter-web.eastmoney.com/api/data/v1/get")
                    .newBuilder()
                    .addQueryParameter("callback", "callback")
                    .addQueryParameter("sortColumns", "TURNOVERRATE,TRADE_DATE,SECURITY_CODE")
                    .addQueryParameter("sortTypes", "-1,-1,1")
                    .addQueryParameter("pageSize", "500")
                    .addQueryParameter("pageNumber", "1")
                    .addQueryParameter("reportName", "RPT_DAILYBILLBOARD_DETAILSNEW")
                    .addQueryParameter("columns", "SECURITY_CODE,SECUCODE,SECURITY_NAME_ABBR,TRADE_DATE,EXPLAIN,CLOSE_PRICE,CHANGE_RATE,BILLBOARD_NET_AMT,BILLBOARD_BUY_AMT,BILLBOARD_SELL_AMT,BILLBOARD_DEAL_AMT,ACCUM_AMOUNT,DEAL_NET_RATIO,DEAL_AMOUNT_RATIO,TURNOVERRATE,FREE_MARKET_CAP,EXPLANATION,D1_CLOSE_ADJCHRATE,D2_CLOSE_ADJCHRATE,D5_CLOSE_ADJCHRATE,D10_CLOSE_ADJCHRATE,SECURITY_TYPE_CODE")
                    .addQueryParameter("source", "WEB")
                    .addQueryParameter("client", "WEB");
            
            // 只有指定了日期才添加日期过滤
            if (queryDate != null && !queryDate.isEmpty()) {
                urlBuilder.addQueryParameter("filter", String.format("(TRADE_DATE<='%s')(TRADE_DATE>='%s')", queryDate, queryDate));
            }
            
            HttpUrl url = urlBuilder.build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "datacenter-web.eastmoney.com")
                    .addHeader("Referer", "https://data.eastmoney.com/stock/tradedetail.html")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取龙虎榜失败: HTTP " + response.code());
                    result.put("error", "HTTP " + response.code());
                    result.put("data", new ArrayList<>());
                    return result;
                }

                String body = response.body().string();
                System.out.println("龙虎榜API响应长度: " + body.length() + ", date=" + queryDate);
                // 去掉 callback 包装，得到纯 JSON
                int start = body.indexOf('(');
                int end = body.lastIndexOf(')');
                if (start > 0 && end > start) {
                    body = body.substring(start + 1, end);
                }

                JsonNode root = objectMapper.readTree(body);
                JsonNode dataNode = root.path("result").path("data");
                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        Map<String, Object> item = objectMapper.convertValue(
                                node, new TypeReference<Map<String, Object>>() {});
                        dragonTigerList.add(item);
                    }
                    System.out.println("成功获取龙虎榜数量: " + dragonTigerList.size());
                } else {
                    System.err.println("龙虎榜API返回的数据格式不正确");
                }
            }

            result.put("data", dragonTigerList);
            result.put("date", queryDate);
            result.put("count", dragonTigerList.size());
        } catch (Exception e) {
            System.err.println("获取龙虎榜异常: " + e.getMessage());
            result.put("error", e.getMessage());
            result.put("data", new ArrayList<>());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getStockResearchReport(String stockCode) {
        List<Map<String, Object>> researchReports = new ArrayList<>();

        try {
            // 参照 Go 版 StockResearchReport，实现东方财富研报列表接口
            if (stockCode == null) {
                stockCode = "";
            }
            String code = stockCode.toLowerCase()
                    .replace("sh", "")
                    .replace("sz", "")
                    .replace("gb_", "")
                    .replace("us_", "")
                    .replace("us", "");

            Calendar calendar = Calendar.getInstance();
            String endDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
            calendar.add(Calendar.YEAR, -1);
            String beginDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());

            String url = "https://reportapi.eastmoney.com/report/list2";

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("code", code);
            reqBody.put("industryCode", "*");
            reqBody.put("beginTime", beginDate);
            reqBody.put("endTime", endDate);
            reqBody.put("pageNo", 1);
            reqBody.put("pageSize", 50);
            reqBody.put("p", 1);
            reqBody.put("pageNum", 1);
            reqBody.put("pageNumber", 1);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "reportapi.eastmoney.com")
                    .addHeader("Origin", "https://data.eastmoney.com")
                    .addHeader("Referer", "https://data.eastmoney.com/report/stock.jshtml")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(
                            objectMapper.writeValueAsString(reqBody),
                            okhttp3.MediaType.parse("application/json;charset=UTF-8")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取个股研报失败: HTTP " + response.code());
                    return researchReports;
                }
                String responseBody = response.body().string();
                System.out.println("个股研报API响应: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode dataNode = root.path("data");
                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        Map<String, Object> item = objectMapper.convertValue(
                                node, new TypeReference<Map<String, Object>>() {});
                        researchReports.add(item);
                    }
                    System.out.println("成功获取个股研报数量: " + researchReports.size());
                } else {
                    System.err.println("个股研报API返回的数据格式不正确，data字段不是数组");
                }
            }
        } catch (Exception e) {
            System.err.println("获取个股研报失败: " + e.getMessage());
            e.printStackTrace();
        }

        return researchReports;
    }

    @Override
    public List<Map<String, Object>> getStockMoneyFlow(String stockCode) {
        // 预留：如果后续前端需要使用「资金流向明细」，可以在此对接对应数据源
        // 目前前端「个股资金流向」页面使用的是 money-trend 接口，因此这里先返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getCompanyNotice(String stockCode) {
        List<Map<String, Object>> noticeList = new ArrayList<>();

        try {
            if (stockCode == null) {
                stockCode = "";
            }
            String code = stockCode.toLowerCase()
                    .replace("sh", "")
                    .replace("sz", "")
                    .replace("gb_", "")
                    .replace("us_", "")
                    .replace("us", "");

            String url = "https://np-anotice-stock.eastmoney.com/api/security/ann";
            HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("page_size", "50")
                    .addQueryParameter("page_index", "1")
                    .addQueryParameter("ann_type", "SHA,CYB,SZA,BJA,INV")
                    .addQueryParameter("client_source", "web")
                    .addQueryParameter("f_node", "0")
                    .addQueryParameter("stock_list", code)
                    .build();

            Request request = new Request.Builder()
                    .url(httpUrl)
                    .addHeader("Host", "np-anotice-stock.eastmoney.com")
                    .addHeader("Referer", "https://data.eastmoney.com/notices/hsa/5.html")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取公司公告失败: HTTP " + response.code());
                    return noticeList;
                }
                String responseBody = response.body().string();
                System.out.println("公司公告API响应长度: " + responseBody.length());
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode dataNode = root.path("data").path("list");
                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        Map<String, Object> item = objectMapper.convertValue(
                                node, new TypeReference<Map<String, Object>>() {});
                        noticeList.add(item);
                    }
                    System.out.println("成功获取公司公告数量: " + noticeList.size());
                } else {
                    System.err.println("公司公告API返回的数据格式不正确");
                }
            }
        } catch (Exception e) {
            System.err.println("获取公司公告失败: " + e.getMessage());
            e.printStackTrace();
        }

        return noticeList;
    }
    
    @Override
    public List<Map<String, Object>> getStockMoneyTrendByDay(String stockCode, int days) {
        List<Map<String, Object>> moneyTrendList = new ArrayList<>();

        try {
            if (stockCode == null) {
                stockCode = "";
            }
            // Go 版直接使用传入的代码，如 "000001" 或 "sh600438"
            String url = String.format(
                    "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/MoneyFlow.ssl_qsfx_zjlrqs?page=1&num=%d&sort=opendate&asc=0&daima=%s",
                    days, stockCode);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "vip.stock.finance.sina.com.cn")
                    .addHeader("Referer", "https://finance.sina.com.cn")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取个股资金趋势失败: HTTP " + response.code());
                    return moneyTrendList;
                }
                String body = response.body().string();
                System.out.println("资金趋势API响应长度: " + body.length() + ", stockCode=" + stockCode);
                // 新浪接口返回的是非标准 JSON（单引号），简单替换后再解析
                body = body.replace("'", "\"");
                JsonNode root = objectMapper.readTree(body);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Map<String, Object> item = objectMapper.convertValue(
                                node, new TypeReference<Map<String, Object>>() {});
                        moneyTrendList.add(item);
                    }
                    System.out.println("成功获取资金趋势数量: " + moneyTrendList.size());
                } else {
                    System.err.println("资金趋势API返回的数据格式不正确，不是数组");
                }
            }
        } catch (Exception e) {
            System.err.println("获取个股资金趋势失败: " + e.getMessage());
            e.printStackTrace();
        }

        return moneyTrendList;
    }
    
    @Override
    public List<Map<String, Object>> getMoneyRankSina(String sort) {
        List<Map<String, Object>> moneyRankList = new ArrayList<>();
        
        try {
            if (sort == null || sort.isEmpty()) {
                sort = "netamount";
            }
            
            // 新浪财经个股资金流向排名API
            String url = String.format(
                "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/MoneyFlow.ssl_bkzj_ssggzj?page=1&num=20&sort=%s&asc=0&bankuai=&shichang=",
                sort);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "vip.stock.finance.sina.com.cn")
                .addHeader("Referer", "https://finance.sina.com.cn")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取个股资金流向排名失败: HTTP " + response.code());
                    return moneyRankList;
                }
                
                String body = response.body().string();
                System.out.println("个股资金流向排名API响应长度: " + body.length() + ", sort=" + sort);
                
                // 新浪接口返回的是非标准 JSON（单引号），简单替换后再解析
                body = body.replace("'", "\"");
                
                JsonNode root = objectMapper.readTree(body);
                
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Map<String, Object> item = objectMapper.convertValue(
                            node, new TypeReference<Map<String, Object>>() {});
                        moneyRankList.add(item);
                    }
                    System.out.println("成功获取个股资金流向排名数量: " + moneyRankList.size());
                } else {
                    System.err.println("个股资金流向排名API返回的数据格式不正确，不是数组");
                }
            }
        } catch (Exception e) {
            System.err.println("获取个股资金流向排名失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return moneyRankList;
    }

    @Override
    public List<Map<String, Object>> getIndustryResearchReport(String industryCode) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            if (industryCode == null) {
                industryCode = "";
            }

            Calendar calendar = Calendar.getInstance();
            String endDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
            calendar.add(Calendar.YEAR, -1);
            String beginDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());

            HttpUrl url = HttpUrl.parse("https://reportapi.eastmoney.com/report/list")
                    .newBuilder()
                    .addQueryParameter("industry", "*")
                    .addQueryParameter("industryCode", industryCode)
                    .addQueryParameter("beginTime", beginDate)
                    .addQueryParameter("endTime", endDate)
                    .addQueryParameter("pageNo", "1")
                    .addQueryParameter("pageSize", "50")
                    .addQueryParameter("p", "1")
                    .addQueryParameter("pageNum", "1")
                    .addQueryParameter("pageNumber", "1")
                    .addQueryParameter("qType", "1")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "reportapi.eastmoney.com")
                    .addHeader("Origin", "https://data.eastmoney.com")
                    .addHeader("Referer", "https://data.eastmoney.com/report/industry.jshtml")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                JsonNode dataNode = root.path("data");
                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        Map<String, Object> item = objectMapper.convertValue(
                                node, new TypeReference<Map<String, Object>>() {});
                        list.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取行业研究报告失败: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 获取最近一个交易日的日期
     * 通过查询龙虎榜API（不带日期过滤）获取最新数据中的交易日期
     */
    private String getLatestTradingDate() {
        try {
            // 不带日期过滤，获取最新的一条龙虎榜数据来确定最近交易日
            HttpUrl url = HttpUrl.parse("https://datacenter-web.eastmoney.com/api/data/v1/get")
                    .newBuilder()
                    .addQueryParameter("callback", "callback")
                    .addQueryParameter("sortColumns", "TRADE_DATE")
                    .addQueryParameter("sortTypes", "-1")
                    .addQueryParameter("pageSize", "1")
                    .addQueryParameter("pageNumber", "1")
                    .addQueryParameter("reportName", "RPT_DAILYBILLBOARD_DETAILSNEW")
                    .addQueryParameter("columns", "TRADE_DATE")
                    .addQueryParameter("source", "WEB")
                    .addQueryParameter("client", "WEB")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "datacenter-web.eastmoney.com")
                    .addHeader("Referer", "https://data.eastmoney.com/stock/tradedetail.html")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    int start = body.indexOf('(');
                    int end = body.lastIndexOf(')');
                    if (start > 0 && end > start) {
                        body = body.substring(start + 1, end);
                    }

                    JsonNode root = objectMapper.readTree(body);
                    JsonNode dataNode = root.path("result").path("data");
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        String tradeDate = dataNode.get(0).path("TRADE_DATE").asText();
                        // 日期格式可能是 "2025-12-31 00:00:00"，需要截取前10位
                        if (tradeDate != null && tradeDate.length() >= 10) {
                            String latestDate = tradeDate.substring(0, 10);
                            System.out.println("获取到最近交易日: " + latestDate);
                            return latestDate;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取最近交易日失败: " + e.getMessage());
        }
        
        // 如果获取失败，返回null，让调用方不带日期过滤查询
        return null;
    }
    
    @Override
    public List<Map<String, Object>> getIndustryMoneyRankSina(String fenlei, String sort) {
        List<Map<String, Object>> rankList = new ArrayList<>();
        
        try {
            if (fenlei == null || fenlei.isEmpty()) {
                fenlei = "0";
            }
            if (sort == null || sort.isEmpty()) {
                sort = "netamount";
            }
            
            // 新浪财经行业资金排名API
            // fenlei: 0-行业, 1-概念, 2-地域
            String url = String.format(
                "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/MoneyFlow.ssl_bkzj_bk?page=1&num=40&sort=%s&asc=0&fenlei=%s",
                sort, fenlei);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "vip.stock.finance.sina.com.cn")
                .addHeader("Referer", "https://finance.sina.com.cn")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取行业资金排名失败: HTTP " + response.code());
                    return rankList;
                }
                
                String body = response.body().string();
                System.out.println("行业资金排名API响应长度: " + body.length() + ", fenlei=" + fenlei + ", sort=" + sort);
                
                // 新浪接口返回的是非标准 JSON（单引号），简单替换后再解析
                body = body.replace("'", "\"");
                
                JsonNode root = objectMapper.readTree(body);
                
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Map<String, Object> item = objectMapper.convertValue(
                            node, new TypeReference<Map<String, Object>>() {});
                        rankList.add(item);
                    }
                    System.out.println("成功获取行业资金排名数量: " + rankList.size());
                } else {
                    System.err.println("行业资金排名API返回的数据格式不正确，不是数组");
                }
            }
        } catch (Exception e) {
            System.err.println("获取行业资金排名失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return rankList;
    }
    
    @Override
    public List<Map<String, Object>> getHotTopic(int size) {
        List<Map<String, Object>> topicList = new ArrayList<>();
        
        try {
            if (size <= 0) {
                size = 10;
            }
            
            // 东方财富股吧热门话题API
            String url = "https://gubatopic.eastmoney.com/interface/GetData.aspx?path=newtopic/api/Topic/HomePageListRead";
            
            okhttp3.FormBody formBody = new okhttp3.FormBody.Builder()
                .add("param", String.format("ps=%d&p=1&type=0", size))
                .add("path", "newtopic/api/Topic/HomePageListRead")
                .add("env", "2")
                .build();
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "gubatopic.eastmoney.com")
                .addHeader("Origin", "https://gubatopic.eastmoney.com")
                .addHeader("Referer", "https://gubatopic.eastmoney.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                .post(formBody)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取热门话题失败: HTTP " + response.code());
                    return topicList;
                }
                
                String body = response.body().string();
                System.out.println("热门话题API响应长度: " + body.length());
                
                JsonNode root = objectMapper.readTree(body);
                JsonNode reNode = root.path("re");
                
                if (reNode.isArray()) {
                    for (JsonNode node : reNode) {
                        Map<String, Object> item = objectMapper.convertValue(
                            node, new TypeReference<Map<String, Object>>() {});
                        topicList.add(item);
                    }
                    System.out.println("成功获取热门话题数量: " + topicList.size());
                } else {
                    System.err.println("热门话题API返回的数据格式不正确");
                }
            }
        } catch (Exception e) {
            System.err.println("获取热门话题失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return topicList;
    }
    
    @Override
    public Map<String, Object> getHotStrategy() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 东方财富热门选股策略API
            String url = String.format(
                "https://np-ipick.eastmoney.com/recommend/stock/heat/ranking?count=20&trace=%d&client=web&biz=web_smart_tag",
                System.currentTimeMillis() / 1000);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "np-ipick.eastmoney.com")
                .addHeader("Origin", "https://xuangu.eastmoney.com")
                .addHeader("Referer", "https://xuangu.eastmoney.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("获取热门策略失败: HTTP " + response.code());
                    return result;
                }
                
                String body = response.body().string();
                System.out.println("热门策略API响应长度: " + body.length());
                
                result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                System.out.println("成功获取热门策略");
            }
        } catch (Exception e) {
            System.err.println("获取热门策略失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> searchStock(String words, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (words == null || words.isEmpty()) {
                result.put("code", -1);
                result.put("message", "请输入选股条件");
                return result;
            }
            
            if (pageSize <= 0) {
                pageSize = 50;
            }
            
            // 东方财富智能选股API
            String url = "https://np-tjxg-g.eastmoney.com/api/smart-tag/stock/v3/pw/search-code";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("keyWord", words);
            requestBody.put("pageSize", pageSize);
            requestBody.put("pageNo", 1);
            requestBody.put("fingerprint", "guest_" + System.currentTimeMillis()); // 使用临时标识
            requestBody.put("gids", new ArrayList<>());
            requestBody.put("matchWord", "");
            requestBody.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            requestBody.put("shareToGuba", false);
            requestBody.put("requestId", "");
            requestBody.put("needCorrect", true);
            requestBody.put("removedConditionIdList", new ArrayList<>());
            requestBody.put("xcId", "");
            requestBody.put("ownSelectAll", false);
            requestBody.put("dxInfo", new ArrayList<>());
            requestBody.put("extraCondition", "");
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "np-tjxg-g.eastmoney.com")
                .addHeader("Origin", "https://xuangu.eastmoney.com")
                .addHeader("Referer", "https://xuangu.eastmoney.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:145.0) Gecko/20100101 Firefox/145.0")
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("智能选股失败: HTTP " + response.code());
                    result.put("code", -1);
                    result.put("message", "请求失败: HTTP " + response.code());
                    return result;
                }
                
                String body = response.body().string();
                System.out.println("智能选股API响应长度: " + body.length());
                
                result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                System.out.println("成功获取选股结果");
            }
        } catch (Exception e) {
            System.err.println("智能选股失败: " + e.getMessage());
            e.printStackTrace();
            result.put("code", -1);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}