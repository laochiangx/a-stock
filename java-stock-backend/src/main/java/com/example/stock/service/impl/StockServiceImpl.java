package com.example.stock.service.impl;

import com.example.stock.dto.StockDTO;
import com.example.stock.service.FollowService;
import com.example.stock.service.StockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StockServiceImpl implements StockService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private FollowService followService;

    @Override
    public List<StockDTO> getStockList(String query) {
        List<StockDTO> stocks = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return stocks;
        }
        
        try {
            // 使用东方财富搜索API
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString());
            String url = "https://searchapi.eastmoney.com/api/suggest/get?input=" + encodedQuery + 
                         "&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=20";
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://www.eastmoney.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    JsonNode data = root.path("QuotationCodeTable").path("Data");
                    
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            StockDTO stock = new StockDTO();
                            String code = item.path("Code").asText("");
                            String name = item.path("Name").asText("");
                            String market = item.path("MktNum").asText("");
                            
                            // 转换市场代码
                            String prefix = "";
                            if ("0".equals(market)) prefix = "sz";
                            else if ("1".equals(market)) prefix = "sh";
                            else if ("2".equals(market)) prefix = "bj";
                            else if ("116".equals(market)) prefix = "hk";
                            
                            stock.setTs_code(prefix + code);
                            stock.setName(name);
                            stocks.add(stock);
                        }
                    }
                    System.out.println("股票搜索结果数量: " + stocks.size() + ", 关键词: " + query);
                }
            }
        } catch (Exception e) {
            System.err.println("股票搜索失败: " + e.getMessage());
        }
        
        return stocks;
    }

    @Override
    public Map<String, Object> getStockRealTimeData(String stockCode) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        try {
            // 尝试从腾讯财经API获取实时数据（更稳定）
            // 格式: http://qt.gtimg.cn/q=sh600519,sz000001
            String url = "http://qt.gtimg.cn/q=" + stockCode;
            
            System.out.println("获取股票数据: " + url);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.qq.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = new String(response.body().bytes(), "GBK");
                    System.out.println("API响应: " + body.substring(0, Math.min(200, body.length())));
                    data = parseTencentStockData(stockCode, body);
                }
            }
            
            // 如果腾讯API失败，尝试新浪API
            if (data.isEmpty() || data.get("股票名称") == null || "".equals(data.get("股票名称"))) {
                System.out.println("腾讯API失败，尝试新浪API");
                String sinaUrl = "http://hq.sinajs.cn/list=" + stockCode;
                Request sinaRequest = new Request.Builder()
                    .url(sinaUrl)
                    .addHeader("Referer", "https://finance.sina.com.cn/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
                
                try (Response response = client.newCall(sinaRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = new String(response.body().bytes(), "GBK");
                        System.out.println("新浪API响应: " + body.substring(0, Math.min(200, body.length())));
                        data = parseSinaStockData(stockCode, body);
                    }
                }
            }
            
            // 合并关注信息（成本价、数量、报警设置等）
            mergeFollowInfo(stockCode, data);
            
        } catch (Exception e) {
            System.err.println("获取股票实时数据失败: " + stockCode + ", " + e.getMessage());
            e.printStackTrace();
            // 返回默认数据
            data = getDefaultStockData(stockCode);
        }
        
        // 确保数据不为空
        if (data.isEmpty()) {
            data = getDefaultStockData(stockCode);
        }
        
        return data;
    }
    
    /**
     * 解析腾讯股票数据
     * 格式: v_sh600519="1~贵州茅台~600519~1810.00~1795.00~1800.00~12345~6789~5678~1810.00~100~1809.00~200~...~20240102150000~...";
     */
    private Map<String, Object> parseTencentStockData(String stockCode, String rawData) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        // 设置默认值
        data.put("股票代码", stockCode);
        data.put("股票名称", "");
        data.put("当前价格", 0.0);
        data.put("涨跌幅", "0.00%");
        data.put("涨跌额", 0.0);
        data.put("今日开盘价", 0.0);
        data.put("今日最高价", 0.0);
        data.put("今日最低价", 0.0);
        data.put("昨日收盘价", 0.0);
        data.put("成交量", 0L);
        data.put("成交额", 0.0);
        data.put("日期", "");
        data.put("时间", "");
        data.put("盘前盘后", "");
        data.put("买一报价", 0.0);
        data.put("卖一报价", 0.0);
        
        try {
            // 提取引号内的数据
            Pattern pattern = Pattern.compile("\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(rawData);
            
            if (matcher.find()) {
                String dataStr = matcher.group(1);
                if (dataStr.isEmpty()) {
                    System.out.println("腾讯API返回空数据");
                    return data;
                }
                
                String[] parts = dataStr.split("~");
                System.out.println("腾讯数据字段数: " + parts.length);
                
                if (parts.length >= 45) {
                    // 腾讯数据格式
                    String name = parts[1];
                    double current = parseDouble(parts[3]);
                    double preClose = parseDouble(parts[4]);
                    double open = parseDouble(parts[5]);
                    long volume = parseLong(parts[6]) * 100; // 腾讯返回的是手，转换为股
                    double amount = parseDouble(parts[37]) * 10000; // 腾讯返回的是万元
                    double high = parseDouble(parts[33]);
                    double low = parseDouble(parts[34]);
                    
                    // 买卖盘口
                    double buy1Price = parseDouble(parts[9]);
                    long buy1Volume = parseLong(parts[10]) * 100;
                    double sell1Price = parseDouble(parts[19]);
                    long sell1Volume = parseLong(parts[20]) * 100;
                    
                    // 日期时间
                    String datetime = parts[30];
                    String date = "";
                    String time = "";
                    if (datetime.length() >= 14) {
                        date = datetime.substring(0, 4) + "-" + datetime.substring(4, 6) + "-" + datetime.substring(6, 8);
                        time = datetime.substring(8, 10) + ":" + datetime.substring(10, 12) + ":" + datetime.substring(12, 14);
                    }
                    
                    // 计算涨跌幅和涨跌额
                    double change = current - preClose;
                    double changePercent = preClose > 0 ? (change / preClose) * 100 : 0;
                    
                    data.put("股票代码", stockCode);
                    data.put("股票名称", name);
                    data.put("当前价格", current);
                    data.put("涨跌幅", String.format("%.2f%%", changePercent));
                    data.put("涨跌额", Math.round(change * 100.0) / 100.0);
                    data.put("今日开盘价", open);
                    data.put("今日最高价", high);
                    data.put("今日最低价", low);
                    data.put("昨日收盘价", preClose);
                    data.put("成交量", volume);
                    data.put("成交额", amount);
                    data.put("日期", date);
                    data.put("时间", time);
                    data.put("盘前盘后", "");
                    
                    // 买卖盘口
                    data.put("买一报价", buy1Price);
                    data.put("买一申报", buy1Volume);
                    data.put("卖一报价", sell1Price);
                    data.put("卖一申报", sell1Volume);
                    
                    // 更多买卖档位
                    if (parts.length >= 29) {
                        data.put("买二报价", parseDouble(parts[11]));
                        data.put("买二申报", parseLong(parts[12]) * 100);
                        data.put("买三报价", parseDouble(parts[13]));
                        data.put("买三申报", parseLong(parts[14]) * 100);
                        data.put("买四报价", parseDouble(parts[15]));
                        data.put("买四申报", parseLong(parts[16]) * 100);
                        data.put("买五报价", parseDouble(parts[17]));
                        data.put("买五申报", parseLong(parts[18]) * 100);
                        
                        data.put("卖二报价", parseDouble(parts[21]));
                        data.put("卖二申报", parseLong(parts[22]) * 100);
                        data.put("卖三报价", parseDouble(parts[23]));
                        data.put("卖三申报", parseLong(parts[24]) * 100);
                        data.put("卖四报价", parseDouble(parts[25]));
                        data.put("卖四申报", parseLong(parts[26]) * 100);
                        data.put("卖五报价", parseDouble(parts[27]));
                        data.put("卖五申报", parseLong(parts[28]) * 100);
                    }
                    
                    // 前端需要的字段
                    data.put("changePercent", Math.round(changePercent * 100.0) / 100.0);
                    data.put("highRate", high > 0 && preClose > 0 ? String.format("%.2f", (high - preClose) / preClose * 100) : "0.00");
                    data.put("lowRate", low > 0 && preClose > 0 ? String.format("%.2f", (low - preClose) / preClose * 100) : "0.00");
                    
                    System.out.println("解析成功: " + name + " 当前价格: " + current);
                }
            }
        } catch (Exception e) {
            System.err.println("解析腾讯股票数据失败: " + stockCode + ", " + e.getMessage());
            e.printStackTrace();
        }
        
        return data;
    }
    
    /**
     * 解析新浪股票数据
     */
    private Map<String, Object> parseSinaStockData(String stockCode, String rawData) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        // 设置默认值
        data.put("股票代码", stockCode);
        data.put("股票名称", "");
        data.put("当前价格", 0.0);
        data.put("涨跌幅", "0.00%");
        data.put("涨跌额", 0.0);
        data.put("今日开盘价", 0.0);
        data.put("今日最高价", 0.0);
        data.put("今日最低价", 0.0);
        data.put("昨日收盘价", 0.0);
        data.put("成交量", 0L);
        data.put("成交额", 0.0);
        data.put("日期", "");
        data.put("时间", "");
        data.put("盘前盘后", "");
        data.put("卖一报价", 0.0);
        data.put("买一报价", 0.0);
        
        try {
            // 提取引号内的数据
            Pattern pattern = Pattern.compile("\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(rawData);
            
            if (matcher.find()) {
                String dataStr = matcher.group(1);
                if (dataStr.isEmpty()) {
                    return data;
                }
                
                String[] parts = dataStr.split(",");
                
                // 判断股票类型并解析
                if (stockCode.startsWith("sh") || stockCode.startsWith("sz") || stockCode.startsWith("bj")) {
                    // A股
                    parseAShareData(data, parts, stockCode);
                } else if (stockCode.startsWith("hk")) {
                    // 港股
                    parseHKShareData(data, parts, stockCode);
                } else if (stockCode.startsWith("gb_")) {
                    // 美股
                    parseUSShareData(data, parts, stockCode);
                }
            }
        } catch (Exception e) {
            System.err.println("解析新浪股票数据失败: " + stockCode + ", " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * 解析A股数据
     * 新浪A股数据格式（33个字段）:
     * 0:股票名称, 1:今日开盘价, 2:昨日收盘价, 3:当前价格, 4:今日最高价, 5:今日最低价,
     * 6:买一报价, 7:卖一报价, 8:成交量(股), 9:成交额(元),
     * 10:买一申报, 11:买一报价, 12:买二申报, 13:买二报价, 14:买三申报, 15:买三报价, 16:买四申报, 17:买四报价, 18:买五申报, 19:买五报价,
     * 20:卖一申报, 21:卖一报价, 22:卖二申报, 23:卖二报价, 24:卖三申报, 25:卖三报价, 26:卖四申报, 27:卖四报价, 28:卖五申报, 29:卖五报价,
     * 30:日期, 31:时间, 32:状态
     */
    private void parseAShareData(Map<String, Object> data, String[] parts, String stockCode) {
        if (parts.length < 32) {
            return;
        }
        
        try {
            String name = parts[0];
            double open = parseDouble(parts[1]);
            double preClose = parseDouble(parts[2]);
            double current = parseDouble(parts[3]);
            double high = parseDouble(parts[4]);
            double low = parseDouble(parts[5]);
            double bid = parseDouble(parts[6]);  // 买一报价
            double ask = parseDouble(parts[7]);  // 卖一报价
            long volume = parseLong(parts[8]);   // 成交量（股）
            double amount = parseDouble(parts[9]); // 成交额
            
            // 买卖五档数据
            long buy1Volume = parseLong(parts[10]);
            double buy1Price = parseDouble(parts[11]);
            long buy2Volume = parseLong(parts[12]);
            double buy2Price = parseDouble(parts[13]);
            long buy3Volume = parseLong(parts[14]);
            double buy3Price = parseDouble(parts[15]);
            long buy4Volume = parseLong(parts[16]);
            double buy4Price = parseDouble(parts[17]);
            long buy5Volume = parseLong(parts[18]);
            double buy5Price = parseDouble(parts[19]);
            
            long sell1Volume = parseLong(parts[20]);
            double sell1Price = parseDouble(parts[21]);
            long sell2Volume = parseLong(parts[22]);
            double sell2Price = parseDouble(parts[23]);
            long sell3Volume = parseLong(parts[24]);
            double sell3Price = parseDouble(parts[25]);
            long sell4Volume = parseLong(parts[26]);
            double sell4Price = parseDouble(parts[27]);
            long sell5Volume = parseLong(parts[28]);
            double sell5Price = parseDouble(parts[29]);
            
            String date = parts.length > 30 ? parts[30] : "";
            String time = parts.length > 31 ? parts[31] : "";
            
            // 计算涨跌幅和涨跌额
            double change = current - preClose;
            double changePercent = preClose > 0 ? (change / preClose) * 100 : 0;
            
            data.put("股票代码", stockCode);
            data.put("股票名称", name);
            data.put("当前价格", current);
            data.put("涨跌幅", String.format("%.2f%%", changePercent));
            data.put("涨跌额", change);
            data.put("今日开盘价", open);
            data.put("今日最高价", high);
            data.put("今日最低价", low);
            data.put("昨日收盘价", preClose);
            data.put("成交量", volume);
            data.put("成交额", amount);
            data.put("日期", date);
            data.put("时间", time);
            data.put("盘前盘后", "");
            
            // 买卖一档
            data.put("买一报价", buy1Price);
            data.put("买一申报", buy1Volume);
            data.put("卖一报价", sell1Price);
            data.put("卖一申报", sell1Volume);
            
            // 买卖二档
            data.put("买二报价", buy2Price);
            data.put("买二申报", buy2Volume);
            data.put("卖二报价", sell2Price);
            data.put("卖二申报", sell2Volume);
            
            // 买卖三档
            data.put("买三报价", buy3Price);
            data.put("买三申报", buy3Volume);
            data.put("卖三报价", sell3Price);
            data.put("卖三申报", sell3Volume);
            
            // 买卖四档
            data.put("买四报价", buy4Price);
            data.put("买四申报", buy4Volume);
            data.put("卖四报价", sell4Price);
            data.put("卖四申报", sell4Volume);
            
            // 买卖五档
            data.put("买五报价", buy5Price);
            data.put("买五申报", buy5Volume);
            data.put("卖五报价", sell5Price);
            data.put("卖五申报", sell5Volume);
            
            // 前端需要的字段
            data.put("changePercent", Math.round(changePercent * 100.0) / 100.0);
            data.put("highRate", high > 0 && preClose > 0 ? String.format("%.2f", (high - preClose) / preClose * 100) : "0.00");
            data.put("lowRate", low > 0 && preClose > 0 ? String.format("%.2f", (low - preClose) / preClose * 100) : "0.00");
            
        } catch (Exception e) {
            System.err.println("解析A股数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析港股数据
     */
    private void parseHKShareData(Map<String, Object> data, String[] parts, String stockCode) {
        if (parts.length < 10) {
            return;
        }
        
        try {
            // 港股数据格式不同，需要适配
            String name = parts[1];
            double current = parseDouble(parts[6]);
            double change = parseDouble(parts[7]);
            double changePercent = parseDouble(parts[8]);
            double high = parseDouble(parts[4]);
            double low = parseDouble(parts[5]);
            double open = parseDouble(parts[2]);
            double preClose = parseDouble(parts[3]);
            
            data.put("股票代码", stockCode);
            data.put("股票名称", name);
            data.put("当前价格", current);
            data.put("涨跌幅", String.format("%.2f%%", changePercent));
            data.put("涨跌额", change);
            data.put("今日开盘价", open);
            data.put("今日最高价", high);
            data.put("今日最低价", low);
            data.put("昨日收盘价", preClose);
            data.put("changePercent", changePercent);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            data.put("日期", sdf.format(now));
            data.put("时间", stf.format(now));
            
        } catch (Exception e) {
            System.err.println("解析港股数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析美股数据
     */
    private void parseUSShareData(Map<String, Object> data, String[] parts, String stockCode) {
        if (parts.length < 20) {
            return;
        }
        
        try {
            // 美股数据格式
            String name = parts[0];
            double current = parseDouble(parts[1]);
            double change = parseDouble(parts[2]);
            double changePercent = parseDouble(parts[3].replace("%", ""));
            double high = parseDouble(parts[6]);
            double low = parseDouble(parts[7]);
            double open = parseDouble(parts[5]);
            double preClose = parseDouble(parts[26]);
            String time = parts.length > 3 ? parts[3] : "";
            
            data.put("股票代码", stockCode);
            data.put("股票名称", name);
            data.put("当前价格", current);
            data.put("涨跌幅", String.format("%.2f%%", changePercent));
            data.put("涨跌额", change);
            data.put("今日开盘价", open);
            data.put("今日最高价", high);
            data.put("今日最低价", low);
            data.put("昨日收盘价", preClose);
            data.put("changePercent", changePercent);
            data.put("盘前盘后", parts.length > 25 ? parts[25] : "");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date now = new Date();
            data.put("日期", sdf.format(now));
            data.put("时间", time);
            
        } catch (Exception e) {
            System.err.println("解析美股数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 合并关注信息
     */
    private void mergeFollowInfo(String stockCode, Map<String, Object> data) {
        try {
            // 获取关注列表中的信息
            List<Map<String, Object>> followList = followService.getFollowList(0);
            for (Map<String, Object> follow : followList) {
                if (stockCode.equals(follow.get("StockCode"))) {
                    // 合并关注信息
                    double costPrice = follow.get("CostPrice") != null ? ((Number) follow.get("CostPrice")).doubleValue() : 0.0;
                    int volume = follow.get("Volume") != null ? ((Number) follow.get("Volume")).intValue() : 0;
                    double alarmChangePercent = follow.get("AlarmChangePercent") != null ? ((Number) follow.get("AlarmChangePercent")).doubleValue() : 0.0;
                    double alarmPrice = follow.get("AlarmPrice") != null ? ((Number) follow.get("AlarmPrice")).doubleValue() : 0.0;
                    int sort = follow.get("Sort") != null ? ((Number) follow.get("Sort")).intValue() : 999;
                    String cron = follow.get("Cron") != null ? follow.get("Cron").toString() : "";
                    
                    data.put("costPrice", costPrice);
                    data.put("volume", volume);  // 持仓数量
                    data.put("costVolume", volume);  // 兼容字段
                    data.put("alarmChangePercent", alarmChangePercent);
                    data.put("alarmPrice", alarmPrice);
                    data.put("sort", sort);
                    data.put("cron", cron);
                    
                    // 计算盈亏
                    double currentPrice = data.get("当前价格") != null ? ((Number) data.get("当前价格")).doubleValue() : 0.0;
                    double preClose = data.get("昨日收盘价") != null ? ((Number) data.get("昨日收盘价")).doubleValue() : 0.0;
                    
                    if (costPrice > 0 && volume > 0 && currentPrice > 0) {
                        double profitAmount = (currentPrice - costPrice) * volume;
                        double profitPercent = (currentPrice - costPrice) / costPrice * 100;
                        data.put("profitAmount", Math.round(profitAmount * 100.0) / 100.0);
                        data.put("profit", String.format("%.2f", profitPercent));
                        
                        // 今日盈亏金额 = (当前价格 - 昨收价) * 持仓数量
                        if (preClose > 0) {
                            double profitAmountToday = (currentPrice - preClose) * volume;
                            data.put("profitAmountToday", Math.round(profitAmountToday * 100.0) / 100.0);
                        } else {
                            data.put("profitAmountToday", 0.0);
                        }
                    } else {
                        data.put("profitAmount", 0.0);
                        data.put("profit", "0.00");
                        data.put("profitAmountToday", 0.0);
                    }
                    
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("合并关注信息失败: " + e.getMessage());
        }
        
        // 设置默认值
        if (!data.containsKey("costPrice")) {
            data.put("costPrice", 0.0);
            data.put("volume", 0);
            data.put("costVolume", 0);
            data.put("alarmChangePercent", 0.0);
            data.put("alarmPrice", 0.0);
            data.put("sort", 999);
            data.put("cron", "");
            data.put("profitAmount", 0.0);
            data.put("profitAmountToday", 0.0);
            data.put("profit", "0.00");
        }
    }
    
    /**
     * 获取默认股票数据
     */
    private Map<String, Object> getDefaultStockData(String stockCode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("股票代码", stockCode);
        data.put("股票名称", "未知");
        data.put("当前价格", 0.0);
        data.put("涨跌幅", "0.00%");
        data.put("涨跌额", 0.0);
        data.put("今日开盘价", 0.0);
        data.put("今日最高价", 0.0);
        data.put("今日最低价", 0.0);
        data.put("昨日收盘价", 0.0);
        data.put("成交量", 0L);
        data.put("成交额", 0.0);
        data.put("日期", "");
        data.put("时间", "");
        data.put("盘前盘后", "");
        data.put("卖一报价", 0.0);
        data.put("买一报价", 0.0);
        data.put("changePercent", 0.0);
        data.put("costPrice", 0.0);
        data.put("volume", 0);
        data.put("costVolume", 0);
        data.put("alarmChangePercent", 0.0);
        data.put("alarmPrice", 0.0);
        data.put("sort", 999);
        data.put("profitAmount", 0.0);
        data.put("profitAmountToday", 0.0);
        data.put("profit", "0.00");
        return data;
    }
    
    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private long parseLong(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0L;
            }
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public List<Map<String, Object>> getStockKLineData(String stockCode, String name, Integer days) {
        List<Map<String, Object>> klineData = new ArrayList<>();
        
        try {
            // 从新浪财经获取K线数据
            // 格式: http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sh600519&scale=240&ma=no&datalen=365
            String scale = "240"; // 日K线
            String url = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=" 
                    + stockCode + "&scale=" + scale + "&ma=no&datalen=" + (days != null ? days : 365);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    
                    // 解析JSON数组
                    if (body != null && body.startsWith("[")) {
                        JsonNode root = objectMapper.readTree(body);
                        if (root.isArray()) {
                            for (JsonNode item : root) {
                                Map<String, Object> kline = new LinkedHashMap<>();
                                kline.put("day", item.path("day").asText(""));
                                kline.put("open", parseDouble(item.path("open").asText("0")));
                                kline.put("close", parseDouble(item.path("close").asText("0")));
                                kline.put("high", parseDouble(item.path("high").asText("0")));
                                kline.put("low", parseDouble(item.path("low").asText("0")));
                                kline.put("volume", parseLong(item.path("volume").asText("0")));
                                klineData.add(kline);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取K线数据失败: " + stockCode + ", " + e.getMessage());
        }
        
        // 如果没有获取到数据，返回模拟数据
        if (klineData.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            int daysCount = days != null ? days : 30;
            
            for (int i = daysCount - 1; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_MONTH, -i);
                
                Map<String, Object> kline = new LinkedHashMap<>();
                kline.put("day", sdf.format(cal.getTime()));
                kline.put("open", 10.0 + (Math.random() * 2));
                kline.put("close", 10.0 + (Math.random() * 2));
                kline.put("high", 10.0 + (Math.random() * 2.5));
                kline.put("low", 9.5 + (Math.random() * 0.5));
                kline.put("volume", (long) (Math.random() * 1000000));
                klineData.add(kline);
            }
        }
        
        return klineData;
    }

    @Override
    public Map<String, Object> getStockMinuteData(String stockCode, String name) {
        Map<String, Object> minuteData = new LinkedHashMap<>();
        List<Map<String, Object>> priceData = new ArrayList<>();
        String date = "";
        
        try {
            // 使用腾讯财经API获取分时数据（和原始Go后端一样）
            // 格式: https://web.ifzq.gtimg.cn/appstock/app/minute/query?code=sh600519
            String queryCode = stockCode;
            String url = "https://web.ifzq.gtimg.cn/appstock/app/minute/query?code=" + queryCode;
            
            // 美股需要特殊处理
            if (stockCode.startsWith("gb_") || stockCode.startsWith("GB_")) {
                queryCode = stockCode.toUpperCase().replace("GB_", "us") + ".OQ";
                url = "https://web.ifzq.gtimg.cn/appstock/app/UsMinute/query?code=" + queryCode;
            } else if (stockCode.startsWith("us") || stockCode.startsWith("US")) {
                url = "https://web.ifzq.gtimg.cn/appstock/app/UsMinute/query?code=" + queryCode;
            }
            
            System.out.println("获取分时数据: " + url);
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "web.ifzq.gtimg.cn")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    System.out.println("分时数据响应长度: " + body.length());
                    
                    // 解析JSON响应
                    JsonNode root = objectMapper.readTree(body);
                    int code = root.path("code").asInt(-1);
                    
                    if (code == 0 && root.has("data")) {
                        JsonNode dataNode = root.path("data").path(queryCode);
                        if (dataNode.isMissingNode()) {
                            // 尝试使用原始stockCode
                            dataNode = root.path("data").path(stockCode);
                        }
                        
                        if (!dataNode.isMissingNode() && dataNode.has("data")) {
                            JsonNode innerData = dataNode.path("data");
                            if (innerData.has("data")) {
                                JsonNode minuteArray = innerData.path("data");
                                date = innerData.path("date").asText("");
                                
                                if (minuteArray.isArray()) {
                                    for (JsonNode item : minuteArray) {
                                        String itemStr = item.asText("");
                                        // 格式: "0930 10.50 1000 10500000" (时间 价格 成交量 成交额)
                                        // 或者: "0930 10.50 1000" (时间 价格 成交量)
                                        String[] parts = itemStr.replace("\r\n", " ").split("\\s+");
                                        
                                        if (parts.length >= 3) {
                                            Map<String, Object> pricePoint = new LinkedHashMap<>();
                                            String timeStr = parts[0];
                                            if (timeStr.length() >= 4) {
                                                timeStr = timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);
                                            }
                                            pricePoint.put("time", timeStr);
                                            pricePoint.put("price", parseDouble(parts[1]));
                                            pricePoint.put("volume", parseDouble(parts[2]));
                                            if (parts.length >= 4) {
                                                pricePoint.put("amount", parseDouble(parts[3]));
                                            } else {
                                                pricePoint.put("amount", 0.0);
                                            }
                                            priceData.add(pricePoint);
                                        }
                                    }
                                    System.out.println("解析分时数据成功，数据点数: " + priceData.size());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取分时数据失败: " + stockCode + ", " + e.getMessage());
            e.printStackTrace();
        }
        
        // 如果没有获取到数据，返回空数组（不返回模拟数据）
        if (date.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            date = sdf.format(new Date());
        }
        
        minuteData.put("date", date);
        minuteData.put("priceData", priceData);
        minuteData.put("stockName", name);
        minuteData.put("stockCode", stockCode);
        
        return minuteData;
    }
}