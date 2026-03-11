package com.example.stock.service.impl;

import com.example.stock.service.IndexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

import okhttp3.*;

@Service
public class IndexServiceImpl implements IndexService {
    
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, List<Map<String, Object>>> getGlobalIndexes() {
        try {
            // 从腾讯财经获取真实数据 - 与Go版本保持一致
            String url = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/rank/indexRankDetail2";
            
            // 设置超时时间
            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://stockapp.finance.qq.com/mstats")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                .build();
            
            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    System.out.println("全球股指API响应长度: " + responseBody.length());
                    
                    // 解析JSON响应
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    
                    // 获取data部分 - Go版本直接返回 res["data"].(map[string]any)
                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        
                        Map<String, List<Map<String, Object>>> result = new HashMap<>();
                        
                        // 解析各个区域的数据
                        for (String key : data.keySet()) {
                            Object regionData = data.get(key);
                            if (regionData instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> rawList = (List<Object>) regionData;
                                List<Map<String, Object>> regionList = new ArrayList<>();
                                
                                for (Object item : rawList) {
                                    if (item instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> itemMap = (Map<String, Object>) item;
                                        Map<String, Object> processedItem = new HashMap<>();
                                        
                                        // 复制所有字段
                                        for (String itemKey : itemMap.keySet()) {
                                            Object value = itemMap.get(itemKey);
                                            
                                            // 将zxj和zdf字段从字符串转换为数字
                                            if ("zxj".equals(itemKey) || "zdf".equals(itemKey)) {
                                                if (value instanceof String) {
                                                    try {
                                                        processedItem.put(itemKey, Double.parseDouble((String) value));
                                                    } catch (NumberFormatException e) {
                                                        processedItem.put(itemKey, 0.0);
                                                    }
                                                } else if (value instanceof Number) {
                                                    processedItem.put(itemKey, ((Number) value).doubleValue());
                                                } else {
                                                    processedItem.put(itemKey, 0.0);
                                                }
                                            } else {
                                                processedItem.put(itemKey, value);
                                            }
                                        }
                                        
                                        regionList.add(processedItem);
                                    }
                                }
                                
                                result.put(key, regionList);
                                System.out.println("解析区域 " + key + " 数据，数量: " + regionList.size());
                            }
                        }
                        
                        return result;
                    }
                } else {
                    System.err.println("全球股指API请求失败: " + response.code());
                }
            }
            
            // 如果API获取失败，返回空Map
            System.err.println("全球股指API获取失败，返回空数据");
            return new HashMap<>();
            
        } catch (Exception e) {
            System.err.println("Error in getGlobalIndexes: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
