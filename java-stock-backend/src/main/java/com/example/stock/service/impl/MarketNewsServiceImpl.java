package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.stock.dao.TagsDao;
import com.example.stock.dao.TelegraphDao;
import com.example.stock.dao.TelegraphTagsDao;
import com.example.stock.dto.MarketNewsDTO;
import com.example.stock.entity.Tags;
import com.example.stock.entity.Telegraph;
import com.example.stock.entity.TelegraphTags;
import com.example.stock.service.MarketNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import okhttp3.*;

@Service
public class MarketNewsServiceImpl implements MarketNewsService {

    private final OkHttpClient client = new OkHttpClient();

    @Autowired
    private TelegraphDao telegraphDao;

    @Autowired
    private TagsDao tagsDao;

    @Autowired
    private TelegraphTagsDao telegraphTagsDao;

    @Override
    public List<MarketNewsDTO> getTelegraphList(String source) {
        List<MarketNewsDTO> newsList = new ArrayList<>();
        
        try {
            // 根据来源获取真实数据
            String actualSource = (source != null && !source.trim().isEmpty()) ? source.trim() : "default";
            
            if ("财联社电报".equals(actualSource)) {
                newsList = getCailianPressNews();
            } else if ("新浪财经".equals(actualSource)) {
                newsList = getSinaFinanceNews();
            } else if ("外媒".equals(actualSource)) {
                newsList = getForeignNews();
            } else if ("股通快速".equals(actualSource)) {
                newsList = getGgtQuickNews();
            } else if ("同花顺快讯".equals(actualSource)) {
                newsList = getThsQuickNews();
            } else {
                // 默认返回财联社数据
                newsList = getCailianPressNews();
            }

            // 持久化操作放在单独的 try-catch 中，失败不影响返回数据
            if ("股通快速".equals(actualSource) || "同花顺快讯".equals(actualSource)) {
                try {
                    persistNewsWithSourceTag(newsList, actualSource);
                } catch (Exception pe) {
                    System.err.println("Error persisting news data (non-fatal): " + pe.getMessage());
                    // 持久化失败不影响返回数据
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in getTelegraphList: " + e.getMessage());
            e.printStackTrace();
            // 返回空列表而不是抛出异常
            return new ArrayList<>();
        }
        
        return newsList;
    }

    // 获取同花顺快讯
    private List<MarketNewsDTO> getThsQuickNews() {
        List<MarketNewsDTO> newsList = new ArrayList<>();

        try {
            String url = "https://news.10jqka.com.cn/app/flash/flashnews/v1/list?seq=0&tagId=-21101";
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://news.10jqka.com.cn/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();

            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> responseMap = mapper.readValue(responseBody, java.util.Map.class);

                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof java.util.Map) {
                        java.util.Map<String, Object> data = (java.util.Map<String, Object>) dataObj;
                        Object listObj = data.get("list");
                        if (listObj instanceof java.util.List) {
                            java.util.List<Object> list = (java.util.List<Object>) listObj;
                            for (Object item : list) {
                                if (!(item instanceof java.util.Map)) {
                                    continue;
                                }
                                java.util.Map<String, Object> newsItem = (java.util.Map<String, Object>) item;

                                MarketNewsDTO news = new MarketNewsDTO();
                                news.setTitle((String) newsItem.get("title"));
                                news.setContent((String) newsItem.get("summary"));
                                news.setSource("同花顺快讯");

                                // 处理时间
                                Object createTimeObj = newsItem.get("createTime");
                                if (createTimeObj != null) {
                                    long timestamp = Long.parseLong(createTimeObj.toString());
                                    LocalDateTime dataTime = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochSecond(timestamp),
                                        java.time.ZoneId.systemDefault()
                                    );
                                    news.setDataTime(dataTime);
                                    news.setTime(dataTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                                }

                                // 检查是否包含涨跌信息来标记重要性
                                String title = news.getTitle() != null ? news.getTitle() : "";
                                boolean isHot = title.contains("涨停") || title.contains("跌停") || 
                                               title.contains("大涨") || title.contains("大跌") ||
                                               title.contains("暴涨") || title.contains("暴跌");
                                news.setRed(isHot);

                                news.setUrl("");
                                
                                // 处理股票标签
                                List<String> stocks = new ArrayList<>();
                                Object stocksObj = newsItem.get("stocks");
                                if (stocksObj instanceof java.util.List) {
                                    for (Object stockItem : (java.util.List<Object>) stocksObj) {
                                        if (stockItem instanceof java.util.Map) {
                                            java.util.Map<String, Object> stock = (java.util.Map<String, Object>) stockItem;
                                            String stockName = (String) stock.get("name");
                                            if (stockName != null && !stockName.isEmpty()) {
                                                stocks.add(stockName);
                                            }
                                        }
                                    }
                                }
                                news.setStocks(stocks);

                                // 处理新闻标签
                                List<String> subjects = new ArrayList<>();
                                Object tagsObj = newsItem.get("tags");
                                if (tagsObj instanceof java.util.List) {
                                    for (Object tagItem : (java.util.List<Object>) tagsObj) {
                                        if (tagItem instanceof java.util.Map) {
                                            java.util.Map<String, Object> tag = (java.util.Map<String, Object>) tagItem;
                                            String tagName = (String) tag.get("name");
                                            if (tagName != null && !tagName.isEmpty()) {
                                                subjects.add(tagName);
                                            }
                                        }
                                    }
                                }
                                news.setSubjects(subjects);

                                // 确保所有字段不为null
                                if (news.getTitle() == null) news.setTitle("");
                                if (news.getContent() == null) news.setContent("");
                                if (news.getTime() == null) news.setTime("");
                                if (news.getUrl() == null) news.setUrl("");
                                if (news.getSource() == null) news.setSource("");
                                if (news.getSentimentResult() == null) news.setSentimentResult("");
                                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                                if (news.getStocks() == null) news.setStocks(new ArrayList<>());

                                newsList.add(news);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting THS quick news: " + e.getMessage());
            e.printStackTrace();

            // 返回示例数据
            for (int i = 1; i <= 5; i++) {
                MarketNewsDTO news = new MarketNewsDTO();
                news.setId((long) i);
                news.setTime(java.time.LocalTime.now().minusSeconds(i * 10).toString().substring(0, 5));
                news.setDataTime(LocalDateTime.now().minusMinutes(i * 5));
                news.setTitle("同花顺快讯：获取数据失败，显示示例" + i);
                news.setContent("无法获取同花顺实时数据，请检查网络连接。");
                news.setUrl("");
                news.setSource("同花顺快讯");
                news.setRed(i % 3 == 0);
                news.setSubjects(new ArrayList<>());
                news.setStocks(new ArrayList<>());
                if (news.getSentimentResult() == null) news.setSentimentResult("");
                newsList.add(news);
            }
        }

        return newsList;
    }

    // 获取股通快速新闻（选股宝快讯）
    private List<MarketNewsDTO> getGgtQuickNews() {
        List<MarketNewsDTO> newsList = new ArrayList<>();

        try {
            String url = "https://baoer-api.xuangubao.cn/api/v6/message/newsflash?subj_ids=9,10,723,35,469,821&platform=pcweb";
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://xuangubao.cn/")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();

            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> responseMap = mapper.readValue(responseBody, java.util.Map.class);

                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof java.util.Map) {
                        java.util.Map<String, Object> data = (java.util.Map<String, Object>) dataObj;
                        Object messagesObj = data.get("messages");
                        if (messagesObj instanceof java.util.List) {
                            java.util.List<Object> messages = (java.util.List<Object>) messagesObj;
                            for (Object item : messages) {
                                if (!(item instanceof java.util.Map)) {
                                    continue;
                                }
                                java.util.Map<String, Object> newsItem = (java.util.Map<String, Object>) item;

                                MarketNewsDTO news = new MarketNewsDTO();
                                news.setTitle((String) newsItem.get("title"));
                                news.setContent((String) newsItem.get("summary"));
                                news.setSource("股通快速");
                                news.setSource(news.getSource() == null ? "" : news.getSource().trim());

                                Object createdAtObj = newsItem.get("created_at");
                                if (createdAtObj != null) {
                                    long timestamp = Long.parseLong(createdAtObj.toString());
                                    LocalDateTime dataTime = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochSecond(timestamp),
                                        java.time.ZoneId.systemDefault()
                                    );
                                    news.setDataTime(dataTime);
                                    news.setTime(dataTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                                }

                                Object subjIdsObj = newsItem.get("subj_ids");
                                boolean isHot = false;
                                if (subjIdsObj instanceof java.util.List) {
                                    for (Object subj : (java.util.List<Object>) subjIdsObj) {
                                        if (subj != null && "10".equals(subj.toString())) {
                                            isHot = true;
                                            break;
                                        }
                                    }
                                }
                                news.setRed(isHot);

                                news.setUrl("");
                                news.setSubjects(new ArrayList<>());
                                news.setStocks(new ArrayList<>());

                                if (news.getTitle() == null) news.setTitle("");
                                if (news.getContent() == null) news.setContent("");
                                if (news.getTime() == null) news.setTime("");
                                if (news.getUrl() == null) news.setUrl("");
                                if (news.getSource() == null) news.setSource("");
                                if (news.getSentimentResult() == null) news.setSentimentResult("");
                                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                                if (news.getStocks() == null) news.setStocks(new ArrayList<>());

                                newsList.add(news);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting GGT quick news: " + e.getMessage());
            e.printStackTrace();

            for (int i = 1; i <= 5; i++) {
                MarketNewsDTO news = new MarketNewsDTO();
                news.setId((long) i);
                news.setTime(java.time.LocalTime.now().minusSeconds(i * 10).toString().substring(0, 5));
                news.setDataTime(LocalDateTime.now().minusMinutes(i * 5));
                news.setTitle("股通快速：获取数据失败，显示示例" + i);
                news.setContent("无法获取股通快速实时数据，请检查网络连接。" + i + "。\n市场出现新动态，请关注重点板块与资金流向变化。");
                news.setUrl("");
                news.setSource("股通快速");
                news.setSource(news.getSource() == null ? "" : news.getSource().trim());
                news.setRed(i % 3 == 0);
                news.setSubjects(new ArrayList<>());
                news.setStocks(new ArrayList<>());
                if (news.getSentimentResult() == null) news.setSentimentResult("");
                newsList.add(news);
            }
        }

        return newsList;
    }

    private void persistNewsWithSourceTag(List<MarketNewsDTO> newsList, String sourceTagName) {
        if (newsList == null || newsList.isEmpty()) {
            return;
        }

        Long tagId = ensureTagExists(sourceTagName, "source");
        if (tagId == null) {
            return;
        }

        for (MarketNewsDTO dto : newsList) {
            if (dto == null) {
                continue;
            }

            Long telegraphId = ensureTelegraphExists(dto);
            if (telegraphId == null) {
                continue;
            }
            dto.setId(telegraphId);

            QueryWrapper<TelegraphTags> linkQw = new QueryWrapper<>();
            linkQw.eq("telegraph_id", telegraphId).eq("tag_id", tagId);
            TelegraphTags existingLink = telegraphTagsDao.selectOne(linkQw);
            if (existingLink == null) {
                TelegraphTags link = new TelegraphTags();
                link.setTelegraphId(telegraphId);
                link.setTagId(tagId);
                link.setCreatedAt(LocalDateTime.now());
                link.setUpdatedAt(LocalDateTime.now());
                link.setDeleted(0);
                telegraphTagsDao.insert(link);
            }
        }
    }

    private Long ensureTagExists(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        QueryWrapper<Tags> qw = new QueryWrapper<>();
        qw.eq("name", name.trim());
        Tags existing = tagsDao.selectOne(qw);
        if (existing != null) {
            return existing.getId();
        }

        Tags tag = new Tags();
        tag.setName(name.trim());
        tag.setType(type);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        tag.setDeleted(0);
        tagsDao.insert(tag);
        return tag.getId();
    }

    private Long ensureTelegraphExists(MarketNewsDTO dto) {
        if (dto == null) {
            return null;
        }

        String title = dto.getTitle() == null ? "" : dto.getTitle();
        String source = dto.getSource() == null ? "" : dto.getSource();

        QueryWrapper<Telegraph> qw = new QueryWrapper<>();
        qw.eq("source", source).eq("title", title);
        if (dto.getDataTime() != null) {
            qw.eq("data_time", dto.getDataTime());
        }
        Telegraph existing = telegraphDao.selectOne(qw);
        if (existing != null) {
            return existing.getId();
        }

        Telegraph telegraph = new Telegraph();
        telegraph.setSource(source);
        telegraph.setTitle(title);
        telegraph.setContent(dto.getContent());
        telegraph.setUrl(dto.getUrl());
        telegraph.setRed(dto.isRed());
        telegraph.setTime(dto.getTime());
        telegraph.setDataTime(dto.getDataTime());
        telegraph.setCreatedAt(LocalDateTime.now());
        telegraph.setUpdatedAt(LocalDateTime.now());
        telegraph.setDeleted(0);

        telegraphDao.insert(telegraph);
        return telegraph.getId();
    }
    
    // 获取财联社真实数据
    private List<MarketNewsDTO> getCailianPressNews() {
        List<MarketNewsDTO> newsList = new ArrayList<>();
        
        try {
            // 从财联社API获取真实数据
            String url = "https://www.cls.cn/nodeapi/telegraphList";
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://www.cls.cn/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                .build();
            
            // 设置更长的超时时间
            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    
                    // 解析JSON响应
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> responseMap = mapper.readValue(responseBody, java.util.Map.class);
                    
                    if (responseMap.get("error") != null && Integer.parseInt(responseMap.get("error").toString()) == 0) {
                        Object dataObj = responseMap.get("data");
                        if (dataObj instanceof java.util.Map) {
                            java.util.Map<String, Object> data = (java.util.Map<String, Object>) dataObj;
                            Object rollDataObj = data.get("roll_data");
                            if (rollDataObj instanceof java.util.List) {
                                java.util.List<Object> rollData = (java.util.List<Object>) rollDataObj;
                                
                                for (Object item : rollData) {
                                    if (item instanceof java.util.Map) {
                                        java.util.Map<String, Object> newsItem = (java.util.Map<String, Object>) item;
                                        
                                        MarketNewsDTO news = new MarketNewsDTO();
                                        news.setTitle((String) newsItem.get("title"));
                                        news.setContent((String) newsItem.get("content"));
                                        
                                        // 处理时间
                                        Object ctime = newsItem.get("ctime");
                                        if (ctime != null) {
                                            long timestamp = Long.parseLong(ctime.toString());
                                            LocalDateTime dataTime = LocalDateTime.ofInstant(
                                                java.time.Instant.ofEpochSecond(timestamp), 
                                                java.time.ZoneId.systemDefault()
                                            );
                                            news.setDataTime(dataTime);
                                            news.setTime(dataTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                                        }
                                        
                                        news.setUrl((String) newsItem.get("shareurl"));
                                        news.setSource("财联社电报");
                                        
                                        // 处理重要性标记
                                        String level = (String) newsItem.get("level");
                                        news.setRed(level != null && !"C".equals(level));
                                        
                                        // 处理主题标签
                                        Object subjectsObj = newsItem.get("subjects");
                                        if (subjectsObj instanceof java.util.List) {
                                            java.util.List<Object> subjectsList = (java.util.List<Object>) subjectsObj;
                                            List<String> subjects = new ArrayList<>();
                                            for (Object subject : subjectsList) {
                                                if (subject instanceof java.util.Map) {
                                                    java.util.Map<String, Object> subjectMap = (java.util.Map<String, Object>) subject;
                                                    String subjectName = (String) subjectMap.get("subject_name");
                                                    if (subjectName != null) {
                                                        subjects.add(subjectName);
                                                    }
                                                }
                                            }
                                            news.setSubjects(subjects);
                                        } else {
                                            news.setSubjects(new ArrayList<>());
                                        }
                                        
                                        news.setStocks(new ArrayList<>()); // 暂时设置为空
                                        
                                        // 确保所有字段都不为null
                                        if (news.getTitle() == null) news.setTitle("");
                                        if (news.getContent() == null) news.setContent("");
                                        if (news.getTime() == null) news.setTime("");
                                        if (news.getUrl() == null) news.setUrl("");
                                        if (news.getSource() == null) news.setSource("");
                                        if (news.getSentimentResult() == null) news.setSentimentResult("");
                                        if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                                        if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                                        
                                        newsList.add(news);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Cailian Press news: " + e.getMessage());
            e.printStackTrace();
            
            // 如果API获取失败，返回一些示例数据
            for (int i = 1; i <= 5; i++) {
                MarketNewsDTO news = new MarketNewsDTO();
                news.setId((long) i);
                news.setTime(java.time.LocalTime.now().minusSeconds(i * 10).toString().substring(0, 5));
                news.setDataTime(LocalDateTime.now().minusMinutes(i * 5));
                news.setTitle("财联社电报：获取数据失败，显示示例" + i);
                news.setContent("无法获取财联社实时数据，请检查网络连接。" + i + "。市场分析显示，今日A股走势强劲，投资者情绪积极。相关板块包括科技股、消费股等。");
                news.setUrl("");
                news.setSource("财联社电报");
                news.setRed(i % 3 == 0);
                
                List<String> subjects = new ArrayList<>();
                subjects.add("A股");
                subjects.add("市场分析");
                news.setSubjects(subjects);
                
                List<String> stocks = new ArrayList<>();
                stocks.add("000001");
                stocks.add("600000");
                news.setStocks(stocks);
                
                // 确保所有字段都不为null
                if (news.getTitle() == null) news.setTitle("");
                if (news.getContent() == null) news.setContent("");
                if (news.getTime() == null) news.setTime("");
                if (news.getUrl() == null) news.setUrl("");
                if (news.getSource() == null) news.setSource("");
                if (news.getSentimentResult() == null) news.setSentimentResult("");
                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                
                newsList.add(news);
            }
        }
        
        return newsList;
    }
    
    // 获取新浪财经真实数据
    private List<MarketNewsDTO> getSinaFinanceNews() {
        List<MarketNewsDTO> newsList = new ArrayList<>();
        
        try {
            // 从新浪财经API获取真实数据
            long timestamp = System.currentTimeMillis();
            String url = String.format("https://zhibo.sina.com.cn/api/zhibo/feed?callback=callback&page=1&page_size=20&zhibo_id=152&tag_id=0&dire=f&dpc=1&pagesize=20&id=4161089&type=0&_=%d", timestamp);
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Referer", "https://finance.sina.com.cn")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60")
                .build();
            
            // 设置更长的超时时间
            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    
                    // 处理JSONP格式的响应
                    String jsonStr = responseBody
                        .replaceFirst("^try\\{callback\\(", "var data=")
                        .replaceFirst("\\);\\}\ncatch\\(e\\)\\{\\};$", ";");
                    
                    // 使用JavaScript引擎解析 - 替换为简单的JSON处理
                    String cleanJson = jsonStr
                        .replace("var data=", "")
                        .replaceAll(";+$", "");
                    
                    // 解析JSON
                    com.fasterxml.jackson.databind.ObjectMapper sinaMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> sinaResponseMap = sinaMapper.readValue(cleanJson, java.util.Map.class);
                    
                    java.util.Map<String, Object> result = (java.util.Map<String, Object>) sinaResponseMap.get("result");
                    if (result != null) {
                        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.get("data");
                        if (data != null) {
                            Object feedObj = data.get("feed");
                            if (feedObj instanceof java.util.Map) {
                                java.util.Map<String, Object> feed = (java.util.Map<String, Object>) feedObj;
                                Object listObj = feed.get("list");
                                if (listObj instanceof java.util.List) {
                                    java.util.List<Object> list = (java.util.List<Object>) listObj;
                                    
                                    for (Object item : list) {
                                        if (item instanceof java.util.Map) {
                                            java.util.Map<String, Object> newsItem = (java.util.Map<String, Object>) item;
                                            
                                            MarketNewsDTO news = new MarketNewsDTO();
                                            news.setContent((String) newsItem.get("rich_text"));
                                            
                                            // 提取标题
                                            String content = news.getContent();
                                            if (content != null && content.contains("【") && content.contains("】")) {
                                                int start = content.indexOf("【") + 1;
                                                int end = content.indexOf("】");
                                                if (start < end) {
                                                    news.setTitle(content.substring(start, end));
                                                }
                                            }
                                            
                                            // 处理时间
                                            String createTime = (String) newsItem.get("create_time");
                                            if (createTime != null) {
                                                // 分离日期和时间部分
                                                String[] parts = createTime.split(" ");
                                                if (parts.length == 2) {
                                                    news.setTime(parts[1]); // 取时间部分
                                                    // 解析完整时间
                                                    java.time.LocalDateTime dataTime = java.time.LocalDateTime.parse(createTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                                    news.setDataTime(dataTime);
                                                }
                                            }
                                            
                                            news.setSource("新浪财经");
                                            
                                            // 处理标签
                                            Object tagObj = newsItem.get("tag");
                                            if (tagObj instanceof java.util.List) {
                                                java.util.List<Object> tags = (java.util.List<Object>) tagObj;
                                                List<String> subjectTags = new ArrayList<>();
                                                for (Object tag : tags) {
                                                    if (tag instanceof java.util.Map) {
                                                        java.util.Map<String, Object> tagMap = (java.util.Map<String, Object>) tag;
                                                        String tagName = (String) tagMap.get("name");
                                                        if (tagName != null) {
                                                            subjectTags.add(tagName);
                                                            if ("焦点".equals(tagName)) {
                                                                news.setRed(true); // 标记焦点新闻为红色
                                                            }
                                                        }
                                                    }
                                                }
                                                news.setSubjects(subjectTags);
                                            } else {
                                                news.setSubjects(new ArrayList<>());
                                            }
                                            
                                            news.setStocks(new ArrayList<>()); // 暂时设置为空
                                            
                                            // 确保所有字段都不为null
                                            if (news.getTitle() == null) news.setTitle("");
                                            if (news.getContent() == null) news.setContent("");
                                            if (news.getTime() == null) news.setTime("");
                                            if (news.getUrl() == null) news.setUrl("");
                                            if (news.getSource() == null) news.setSource("");
                                            if (news.getSentimentResult() == null) news.setSentimentResult("");
                                            if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                                            if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                                            
                                            newsList.add(news);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Sina Finance news: " + e.getMessage());
            e.printStackTrace();
            
            // 如果API获取失败，返回一些示例数据
            for (int i = 1; i <= 5; i++) {
                MarketNewsDTO news = new MarketNewsDTO();
                news.setId((long) i);
                news.setTime(java.time.LocalTime.now().minusSeconds(i * 10).toString().substring(0, 5));
                news.setDataTime(LocalDateTime.now().minusMinutes(i * 5));
                news.setTitle("新浪财经：获取数据失败，显示示例" + i);
                news.setContent("无法获取新浪财经实时数据，请检查网络连接。" + i + "。宏观经济数据表现良好，对股市形成支撑。分析师认为后市仍有上涨空间，建议关注相关板块机会。");
                news.setUrl("");
                news.setSource("新浪财经");
                news.setRed(false); // 新浪财经通常不标记红色
                
                List<String> subjects = new ArrayList<>();
                subjects.add("宏观经济");
                subjects.add("股市分析");
                news.setSubjects(subjects);
                
                List<String> stocks = new ArrayList<>();
                stocks.add("000002");
                stocks.add("601398");
                news.setStocks(stocks);
                
                // 确保所有字段都不为null
                if (news.getTitle() == null) news.setTitle("");
                if (news.getContent() == null) news.setContent("");
                if (news.getTime() == null) news.setTime("");
                if (news.getUrl() == null) news.setUrl("");
                if (news.getSource() == null) news.setSource("");
                if (news.getSentimentResult() == null) news.setSentimentResult("");
                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                
                newsList.add(news);
            }
        }
        
        return newsList;
    }
    
    // 获取外网财经数据
    private List<MarketNewsDTO> getForeignNews() {
        List<MarketNewsDTO> newsList = new ArrayList<>();
        
        try {
            // 从TradingView获取真实数据
            String url = "https://news-mediator.tradingview.com/news-flow/v2/news?filter=lang%3Azh-Hans&client=screener&streaming=false";
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "news-mediator.tradingview.com")
                .addHeader("Origin", "https://cn.tradingview.com")
                .addHeader("Referer", "https://cn.tradingview.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                .build();
            
            // 设置更长的超时时间
            OkHttpClient timeoutClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
            
            try (Response response = timeoutClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    
                    // 解析JSON
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> responseMap = mapper.readValue(responseBody, java.util.Map.class);
                    
                    Object itemsObj = responseMap.get("items");
                    if (itemsObj instanceof java.util.List) {
                        java.util.List<Object> items = (java.util.List<Object>) itemsObj;
                        
                        for (int i = 0; i < Math.min(items.size(), 10); i++) { // 只取前10条
                            Object item = items.get(i);
                            if (item instanceof java.util.Map) {
                                java.util.Map<String, Object> newsItem = (java.util.Map<String, Object>) item;
                                
                                MarketNewsDTO news = new MarketNewsDTO();
                                news.setTitle((String) newsItem.get("title"));
                                
                                // 处理时间
                                Object published = newsItem.get("published");
                                if (published != null) {
                                    long timestamp = Long.parseLong(published.toString());
                                    LocalDateTime dataTime = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochSecond(timestamp), 
                                        java.time.ZoneId.systemDefault()
                                    );
                                    news.setDataTime(dataTime);
                                    news.setTime(dataTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                                }
                                
                                news.setSource("外媒");
                                
                                // 构造URL
                                String id = (String) newsItem.get("id");
                                if (id != null) {
                                    news.setUrl(String.format("https://cn.tradingview.com/news/%s", id));
                                }
                                
                                news.setRed(false); // 外媒通常不标记红色
                                
                                // 暂时设置空列表
                                news.setSubjects(new ArrayList<>());
                                news.setStocks(new ArrayList<>());
                                
                                // 确保所有字段都不为null
                                if (news.getTitle() == null) news.setTitle("");
                                if (news.getContent() == null) news.setContent("");
                                if (news.getTime() == null) news.setTime("");
                                if (news.getUrl() == null) news.setUrl("");
                                if (news.getSource() == null) news.setSource("");
                                if (news.getSentimentResult() == null) news.setSentimentResult("");
                                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                                if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                                
                                newsList.add(news);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting foreign news: " + e.getMessage());
            e.printStackTrace();
            
            // 如果API获取失败，返回一些示例数据
            for (int i = 1; i <= 5; i++) {
                MarketNewsDTO news = new MarketNewsDTO();
                news.setId((long) i);
                news.setTime(java.time.LocalTime.now().minusSeconds(i * 10).toString().substring(0, 5));
                news.setDataTime(LocalDateTime.now().minusMinutes(i * 5));
                news.setTitle("海外财经：获取数据失败，显示示例" + i);
                news.setContent("无法获取海外财经实时数据，请检查网络连接。" + i + "。国际市场出现波动，影响A股情绪。美联储政策预期对全球市场产生重要影响，投资者需关注相关变化。");
                news.setUrl("");
                news.setSource("外媒");
                news.setRed(i % 4 == 0); // 标记重要新闻
                
                List<String> subjects = new ArrayList<>();
                subjects.add("国际金融");
                subjects.add("美联储");
                news.setSubjects(subjects);
                
                List<String> stocks = new ArrayList<>();
                stocks.add("AAPL");
                stocks.add("GOOGL");
                news.setStocks(stocks);
                
                // 确保所有字段都不为null
                if (news.getTitle() == null) news.setTitle("");
                if (news.getContent() == null) news.setContent("");
                if (news.getTime() == null) news.setTime("");
                if (news.getUrl() == null) news.setUrl("");
                if (news.getSource() == null) news.setSource("");
                if (news.getSentimentResult() == null) news.setSentimentResult("");
                if (news.getSubjects() == null) news.setSubjects(new ArrayList<>());
                if (news.getStocks() == null) news.setStocks(new ArrayList<>());
                
                newsList.add(news);
            }
        }
        
        return newsList;
    }

    @Override
    public List<MarketNewsDTO> refreshTelegraphList(String source) {
        return getTelegraphList(source);
    }

    @Override
    public Map<String, Object> summaryStockNews(String question, Integer configId, String promptId, 
                                               Boolean enableTools, String thinkingMode) {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", "这是模拟的新闻总结结果");
        result.put("question", question);
        result.put("configId", configId);
        result.put("promptId", promptId);
        result.put("enableTools", enableTools);
        result.put("thinkingMode", thinkingMode);
        return result;
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
                
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
                
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        Map<String, Object> item = objectMapper.convertValue(
                            node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
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
}