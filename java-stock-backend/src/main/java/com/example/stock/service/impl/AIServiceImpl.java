package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.HotWordDao;
import com.example.stock.dao.WordDictionaryDao;
import com.example.stock.dto.MarketNewsDTO;
import com.example.stock.entity.HotWord;
import com.example.stock.entity.WordDictionary;
import com.example.stock.service.AIService;
import com.example.stock.service.MarketNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private MarketNewsService marketNewsService;

    @Autowired
    private HotWordDao hotWordDao;

    @Autowired
    private WordDictionaryDao wordDictionaryDao;

    // 内存缓存词典，提高查询效率
    private Map<String, WordDictionary> wordDictCache = new ConcurrentHashMap<>();

    // 停用词集合
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "是", "在", "有", "和", "与", "或", "等", "为", "被", "把", "将",
        "这", "那", "个", "些", "之", "于", "以", "及", "其", "也", "都", "而", "但",
        "如果", "因为", "所以", "虽然", "但是", "然而", "不过", "可以", "可能", "应该",
        "已经", "正在", "将要", "曾经", "一直", "仍然", "还是", "就是", "只是", "不是"
    ));

    // 模拟AI配置
    private static final List<Map<String, Object>> aiConfigs = new ArrayList<>();
    private static final List<Map<String, Object>> promptTemplates = new ArrayList<>();
    private static final List<Map<String, Object>> aiResults = new ArrayList<>();

    static {
        // 初始化AI配置
        Map<String, Object> config1 = new HashMap<>();
        config1.put("ID", 1);
        config1.put("name", "默认配置");
        config1.put("baseUrl", "https://api.deepseek.com");
        config1.put("apiKey", "");
        config1.put("modelName", "deepseek-chat");
        config1.put("temperature", 0.7);
        config1.put("maxTokens", 4096);
        aiConfigs.add(config1);

        // 初始化提示模板
        Map<String, Object> template1 = new HashMap<>();
        template1.put("ID", 1);
        template1.put("name", "股票分析");
        template1.put("content", "请分析这只股票的基本面和技术面");
        template1.put("type", "模型系统Prompt");
        promptTemplates.add(template1);
    }

    /**
     * 初始化系统词典
     */
    @PostConstruct
    public void initSystemDictionary() {
        try {
            // 检查是否已初始化
            long count = wordDictionaryDao.selectCount(new LambdaQueryWrapper<WordDictionary>()
                .eq(WordDictionary::getIsSystem, true));
            if (count > 0) {
                loadDictionaryToCache();
                return;
            }

            // 初始化正面词汇
            initPositiveWords();
            // 初始化负面词汇
            initNegativeWords();
            // 初始化行业词汇
            initIndustryWords();
            // 初始化概念词汇
            initConceptWords();

            loadDictionaryToCache();
            System.out.println("系统词典初始化完成");
        } catch (Exception e) {
            System.err.println("初始化系统词典失败: " + e.getMessage());
        }
    }

    private void initPositiveWords() {
        String[][] positiveWords = {
            // 词汇, 权重, 情感值
            {"上涨", "5.0", "2.0"}, {"涨停", "6.0", "2.5"}, {"大涨", "5.5", "2.2"},
            {"飙升", "5.0", "2.0"}, {"利好", "4.5", "1.7"}, {"盈利", "4.0", "1.6"},
            {"增长", "4.0", "1.5"}, {"强劲", "4.0", "1.5"}, {"买入", "3.5", "1.4"},
            {"推荐", "3.5", "1.3"}, {"看涨", "4.0", "1.3"}, {"突破", "4.5", "1.5"},
            {"创新高", "5.0", "1.8"}, {"强势", "4.0", "1.5"}, {"反弹", "3.5", "1.4"},
            {"复苏", "4.0", "1.5"}, {"回升", "3.5", "1.4"}, {"走强", "4.0", "1.5"},
            {"拉升", "4.5", "1.6"}, {"放量", "3.5", "1.3"}, {"活跃", "3.0", "1.2"},
            {"景气", "3.5", "1.4"}, {"繁荣", "4.0", "1.5"}, {"扩张", "3.5", "1.3"},
            {"提振", "3.5", "1.4"}, {"乐观", "3.5", "1.3"}, {"向好", "3.5", "1.4"},
            {"改善", "3.5", "1.3"}, {"超预期", "4.5", "1.6"}, {"新高", "4.0", "1.5"},
            {"暴涨", "5.5", "2.3"}, {"狂涨", "5.5", "2.3"}, {"井喷", "5.0", "2.0"},
            {"爆发", "4.5", "1.8"}, {"腾飞", "4.5", "1.8"}, {"翻倍", "5.0", "2.0"},
            {"牛市", "5.0", "2.0"}, {"红盘", "3.5", "1.3"}, {"飘红", "3.5", "1.3"},
            {"领涨", "4.0", "1.5"}, {"涨幅", "3.0", "1.0"}, {"高开", "3.0", "1.2"},
            {"封板", "4.5", "1.7"}, {"连板", "5.0", "2.0"}, {"龙头", "4.0", "1.5"}
        };
        for (String[] w : positiveWords) {
            saveSystemWord(w[0], WordDictionary.TYPE_POSITIVE, Double.parseDouble(w[1]), Double.parseDouble(w[2]), null);
        }
    }

    private void initNegativeWords() {
        String[][] negativeWords = {
            {"下跌", "5.0", "-2.0"}, {"跌停", "6.0", "-2.5"}, {"大跌", "5.5", "-2.2"},
            {"暴跌", "5.5", "-2.3"}, {"利空", "4.5", "-1.7"}, {"亏损", "4.0", "-1.6"},
            {"下降", "4.0", "-1.5"}, {"疲软", "4.0", "-1.5"}, {"卖出", "3.5", "-1.4"},
            {"看跌", "4.0", "-1.3"}, {"破位", "4.5", "-1.5"}, {"创新低", "5.0", "-1.8"},
            {"弱势", "4.0", "-1.5"}, {"回调", "3.5", "-1.4"}, {"衰退", "4.0", "-1.5"},
            {"下滑", "3.5", "-1.4"}, {"走弱", "4.0", "-1.5"}, {"杀跌", "4.5", "-1.6"},
            {"缩量", "3.5", "-1.3"}, {"低迷", "3.5", "-1.4"}, {"萧条", "4.0", "-1.5"},
            {"收缩", "3.5", "-1.3"}, {"承压", "3.5", "-1.4"}, {"悲观", "3.5", "-1.3"},
            {"恶化", "3.5", "-1.4"}, {"不及预期", "4.5", "-1.6"}, {"新低", "4.0", "-1.5"},
            {"风险", "3.5", "-1.3"}, {"危机", "4.5", "-1.6"}, {"崩盘", "5.5", "-2.0"},
            {"熊市", "5.0", "-2.0"}, {"绿盘", "3.5", "-1.3"}, {"飘绿", "3.5", "-1.3"},
            {"领跌", "4.0", "-1.5"}, {"跌幅", "3.0", "-1.0"}, {"低开", "3.0", "-1.2"},
            {"炸板", "4.5", "-1.7"}, {"闪崩", "5.0", "-2.0"}, {"踩踏", "5.0", "-2.0"},
            {"割肉", "4.0", "-1.5"}, {"套牢", "4.0", "-1.5"}, {"爆仓", "5.5", "-2.3"}
        };
        for (String[] w : negativeWords) {
            saveSystemWord(w[0], WordDictionary.TYPE_NEGATIVE, Double.parseDouble(w[1]), Double.parseDouble(w[2]), null);
        }
    }

    private void initIndustryWords() {
        String[][] industryWords = {
            // 词汇, 权重, 行业
            {"银行", "3.0", "金融"}, {"保险", "3.0", "金融"}, {"证券", "3.0", "金融"},
            {"券商", "3.0", "金融"}, {"基金", "3.0", "金融"}, {"信托", "3.0", "金融"},
            {"地产", "3.0", "房地产"}, {"房企", "3.0", "房地产"}, {"楼市", "3.0", "房地产"},
            {"医药", "3.0", "医药"}, {"医疗", "3.0", "医药"}, {"生物", "3.0", "医药"},
            {"制药", "3.0", "医药"}, {"疫苗", "3.5", "医药"}, {"创新药", "3.5", "医药"},
            {"白酒", "3.0", "消费"}, {"食品", "3.0", "消费"}, {"饮料", "3.0", "消费"},
            {"家电", "3.0", "消费"}, {"汽车", "3.0", "汽车"}, {"新能源车", "3.5", "汽车"},
            {"电动车", "3.5", "汽车"}, {"锂电", "3.5", "新能源"}, {"光伏", "3.5", "新能源"},
            {"风电", "3.5", "新能源"}, {"储能", "3.5", "新能源"}, {"氢能", "3.5", "新能源"},
            {"芯片", "4.0", "半导体"}, {"半导体", "4.0", "半导体"}, {"集成电路", "4.0", "半导体"},
            {"晶圆", "3.5", "半导体"}, {"封测", "3.5", "半导体"}, {"光刻", "4.0", "半导体"},
            {"人工智能", "4.5", "科技"}, {"大模型", "4.5", "科技"}, {"算力", "4.0", "科技"},
            {"云计算", "3.5", "科技"}, {"大数据", "3.5", "科技"}, {"物联网", "3.5", "科技"},
            {"机器人", "4.0", "科技"}, {"无人机", "3.5", "科技"}, {"自动驾驶", "4.0", "科技"},
            {"军工", "3.5", "军工"}, {"航空", "3.5", "军工"}, {"航天", "3.5", "军工"},
            {"钢铁", "3.0", "周期"}, {"煤炭", "3.0", "周期"}, {"有色", "3.0", "周期"},
            {"化工", "3.0", "周期"}, {"建材", "3.0", "周期"}, {"水泥", "3.0", "周期"}
        };
        for (String[] w : industryWords) {
            saveSystemWord(w[0], WordDictionary.TYPE_INDUSTRY, Double.parseDouble(w[1]), 0.0, w[2]);
        }
    }

    private void initConceptWords() {
        String[][] conceptWords = {
            {"低空经济", "4.5", "低空经济"}, {"飞行汽车", "4.0", "低空经济"},
            {"数字经济", "4.0", "数字经济"}, {"数字货币", "4.0", "数字经济"},
            {"元宇宙", "3.5", "元宇宙"}, {"虚拟现实", "3.5", "元宇宙"},
            {"国产替代", "4.0", "国产替代"}, {"自主可控", "4.0", "国产替代"},
            {"碳中和", "4.0", "碳中和"}, {"碳达峰", "4.0", "碳中和"},
            {"专精特新", "3.5", "专精特新"}, {"小巨人", "3.5", "专精特新"},
            {"北交所", "3.5", "北交所"}, {"科创板", "3.5", "科创板"},
            {"创业板", "3.5", "创业板"}, {"注册制", "3.0", "注册制"},
            {"国企改革", "3.5", "国企改革"}, {"央企", "3.5", "国企改革"},
            {"一带一路", "3.5", "一带一路"}, {"东盟", "3.0", "一带一路"},
            {"华为", "4.0", "华为概念"}, {"鸿蒙", "4.0", "华为概念"},
            {"苹果", "3.5", "苹果概念"}, {"特斯拉", "4.0", "特斯拉概念"},
            {"茅台", "4.0", "白酒龙头"}, {"宁德", "4.0", "锂电龙头"},
            {"比亚迪", "4.0", "新能源车"}, {"小米", "3.5", "小米概念"}
        };
        for (String[] w : conceptWords) {
            saveSystemWord(w[0], WordDictionary.TYPE_CONCEPT, Double.parseDouble(w[1]), 0.0, w[2]);
        }
    }

    private void saveSystemWord(String word, String type, double weight, double sentiment, String industry) {
        try {
            WordDictionary dict = new WordDictionary();
            dict.setWord(word);
            dict.setWordType(type);
            dict.setBaseWeight(BigDecimal.valueOf(weight));
            dict.setSentimentValue(BigDecimal.valueOf(sentiment));
            dict.setIndustry(industry);
            dict.setIsSystem(true);
            dict.setFrequencyTotal(0);
            dict.setCreatedAt(LocalDateTime.now());
            dict.setUpdatedAt(LocalDateTime.now());
            wordDictionaryDao.insert(dict);
        } catch (Exception e) {
            // 忽略重复插入错误
        }
    }

    private void loadDictionaryToCache() {
        List<WordDictionary> allWords = wordDictionaryDao.selectList(null);
        wordDictCache.clear();
        for (WordDictionary word : allWords) {
            wordDictCache.put(word.getWord(), word);
        }
        System.out.println("加载词典到缓存，共 " + wordDictCache.size() + " 个词汇");
    }

    @Override
    public List<Map<String, Object>> getAiConfigs() {
        return aiConfigs;
    }

    @Override
    public List<Map<String, Object>> getPromptTemplates(String type, String content) {
        if (type == null && content == null) {
            return promptTemplates;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> template : promptTemplates) {
            boolean match = true;
            if (type != null && !type.isEmpty()) {
                String templateType = (String) template.get("type");
                if (templateType == null || !templateType.contains(type)) match = false;
            }
            if (content != null && !content.isEmpty()) {
                String templateContent = (String) template.get("content");
                if (templateContent == null || !templateContent.contains(content)) match = false;
            }
            if (match) filtered.add(template);
        }
        return filtered;
    }

    @Override
    public String saveAIResponseResult(Map<String, Object> request) {
        aiResults.add(request);
        return "AI响应结果保存成功";
    }

    @Override
    public Map<String, Object> getAIResponseResult(Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("results", aiResults);
        result.put("count", aiResults.size());
        return result;
    }

    @Override
    public Map<String, Object> chatWithAgent(String question, Integer configId, Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("answer", "根据您的问题：'" + question + "'，这是AI的分析结果...");
        response.put("question", question);
        response.put("configId", configId);
        response.put("timestamp", new Date());
        return response;
    }

    @Override
    public Map<String, Object> summaryStockNews(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("summary", "这是对股票新闻的总结...");
        response.put("analysis", "分析内容");
        return response;
    }

    @Override
    public Map<String, Object> analyzeSentimentWithFreqWeight(String text, String source) {
        try {
            // 如果文本为空，从新闻API获取数据
            if (text == null || text.trim().isEmpty()) {
                text = fetchNewsText(source);
                System.out.println("从新闻API获取文本，长度: " + text.length());
            }

            // 分词并统计
            List<WordInfo> wordInfoList = analyzeText(text);

            // 计算情感得分
            Map<String, Object> sentimentResult = calculateSentiment(wordInfoList);

            // 构建词频统计结果（带更多维度）
            List<Map<String, Object>> frequencies = buildFrequencyResult(wordInfoList);

            // 保存热词到数据库（增强版）
            saveHotWordsToDbEnhanced(wordInfoList, source);

            // 自动学习新词汇
            learnNewWords(wordInfoList);

            Map<String, Object> result = new HashMap<>();
            result.put("result", sentimentResult);
            result.put("frequencies", frequencies);

            // 新增：按类型分组的统计
            result.put("byType", groupByType(wordInfoList));
            // 新增：按行业分组的统计
            result.put("byIndustry", groupByIndustry(wordInfoList));

            return result;
        } catch (Exception e) {
            System.err.println("Error in analyzeSentimentWithFreqWeight: " + e.getMessage());
            e.printStackTrace();
            return buildDefaultResult();
        }
    }

    private String fetchNewsText(String source) {
        StringBuilder newsText = new StringBuilder();
        String actualSource = source == null ? "" : source.trim();

        List<String> sources = new ArrayList<>();
        if (actualSource.isEmpty() || "全部".equals(actualSource)) {
            sources.addAll(Arrays.asList("财联社电报", "新浪财经", "股通快速", "同花顺快讯"));
        } else {
            sources.add(actualSource);
        }

        for (String src : sources) {
            try {
                List<MarketNewsDTO> newsList = marketNewsService.getTelegraphList(src);
                for (MarketNewsDTO news : newsList) {
                    if (news.getTitle() != null) newsText.append(news.getTitle()).append(" ");
                    if (news.getContent() != null) newsText.append(news.getContent()).append("\n");
                }
            } catch (Exception e) {
                System.err.println("获取新闻失败: " + src + ", " + e.getMessage());
            }
        }
        return newsText.toString();
    }

    /**
     * 词汇信息内部类
     */
    private static class WordInfo {
        String word;
        int frequency;
        double weight;
        double sentimentValue;
        String wordType;
        String industry;
        boolean isNew; // 是否新词

        WordInfo(String word) {
            this.word = word;
            this.frequency = 1;
            this.weight = 1.0;
            this.sentimentValue = 0.0;
            this.wordType = WordDictionary.TYPE_NORMAL;
            this.industry = null;
            this.isNew = false;
        }
    }

    /**
     * 分析文本，返回词汇信息列表
     */
    private List<WordInfo> analyzeText(String text) {
        Map<String, WordInfo> wordMap = new HashMap<>();

        // 分词
        List<String> words = splitWords(text);

        for (String word : words) {
            if (word.length() < 2 || STOP_WORDS.contains(word)) continue;

            WordInfo info = wordMap.get(word);
            if (info == null) {
                info = new WordInfo(word);
                // 从词典获取信息
                WordDictionary dict = wordDictCache.get(word);
                if (dict != null) {
                    info.weight = dict.getBaseWeight().doubleValue();
                    info.sentimentValue = dict.getSentimentValue() != null ? dict.getSentimentValue().doubleValue() : 0;
                    info.wordType = dict.getWordType();
                    info.industry = dict.getIndustry();
                } else {
                    // 新词，使用默认权重
                    info.weight = 1.0;
                    info.isNew = true;
                    // 尝试推断词汇类型
                    info.wordType = inferWordType(word);
                }
                wordMap.put(word, info);
            } else {
                info.frequency++;
            }
        }

        // 转换为列表并排序
        List<WordInfo> result = new ArrayList<>(wordMap.values());
        result.sort((a, b) -> {
            double scoreA = a.frequency * a.weight;
            double scoreB = b.frequency * b.weight;
            return Double.compare(scoreB, scoreA);
        });

        return result;
    }

    /**
     * 推断词汇类型（简单规则）
     */
    private String inferWordType(String word) {
        // 包含涨跌相关字眼
        if (word.contains("涨") || word.contains("升") || word.contains("增") || word.contains("好")) {
            return WordDictionary.TYPE_POSITIVE;
        }
        if (word.contains("跌") || word.contains("降") || word.contains("减") || word.contains("差")) {
            return WordDictionary.TYPE_NEGATIVE;
        }
        return WordDictionary.TYPE_NORMAL;
    }

    /**
     * 计算情感得分
     */
    private Map<String, Object> calculateSentiment(List<WordInfo> wordInfoList) {
        double totalScore = 0.0;
        int positiveCount = 0;
        int negativeCount = 0;
        double positiveWeight = 0.0;
        double negativeWeight = 0.0;

        for (WordInfo info : wordInfoList) {
            double contribution = info.sentimentValue * info.frequency;
            totalScore += contribution;

            if (info.sentimentValue > 0) {
                positiveCount += info.frequency;
                positiveWeight += info.weight * info.frequency;
            } else if (info.sentimentValue < 0) {
                negativeCount += info.frequency;
                negativeWeight += info.weight * info.frequency;
            }
        }

        int category = getSentimentCategory(totalScore);

        Map<String, Object> result = new HashMap<>();
        result.put("Score", totalScore);
        result.put("Category", category);
        result.put("PositiveCount", positiveCount);
        result.put("NegativeCount", negativeCount);
        result.put("PositiveWeight", positiveWeight);
        result.put("NegativeWeight", negativeWeight);
        result.put("Description", getSentimentDescription(category));
        result.put("Ratio", negativeCount > 0 ? (double) positiveCount / negativeCount : positiveCount);

        return result;
    }

    /**
     * 构建词频统计结果
     */
    private List<Map<String, Object>> buildFrequencyResult(List<WordInfo> wordInfoList) {
        List<Map<String, Object>> frequencies = new ArrayList<>();

        for (WordInfo info : wordInfoList) {
            // 只保留有意义的词（权重>=1且频次>=2，或者是情感词/行业词）
            boolean isImportant = info.weight >= 1.0 && info.frequency >= 2;
            boolean isSentiment = WordDictionary.TYPE_POSITIVE.equals(info.wordType) 
                               || WordDictionary.TYPE_NEGATIVE.equals(info.wordType);
            boolean isIndustry = WordDictionary.TYPE_INDUSTRY.equals(info.wordType)
                              || WordDictionary.TYPE_CONCEPT.equals(info.wordType);

            if (isImportant || isSentiment || isIndustry) {
                Map<String, Object> freqInfo = new HashMap<>();
                freqInfo.put("Word", info.word);
                freqInfo.put("Frequency", info.frequency);
                freqInfo.put("Weight", info.weight);
                freqInfo.put("Score", info.frequency * info.weight);
                freqInfo.put("SentimentValue", info.sentimentValue);
                freqInfo.put("WordType", info.wordType);
                freqInfo.put("Industry", info.industry);
                freqInfo.put("IsNew", info.isNew);

                frequencies.add(freqInfo);
            }
        }

        // 取前100个
        if (frequencies.size() > 100) {
            frequencies = frequencies.subList(0, 100);
        }

        return frequencies;
    }

    /**
     * 按类型分组统计
     */
    private Map<String, Object> groupByType(List<WordInfo> wordInfoList) {
        Map<String, List<Map<String, Object>>> byType = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();
        Map<String, Double> typeScores = new HashMap<>();

        for (WordInfo info : wordInfoList) {
            String type = info.wordType;
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + info.frequency);
            typeScores.put(type, typeScores.getOrDefault(type, 0.0) + info.frequency * info.weight);

            byType.computeIfAbsent(type, k -> new ArrayList<>());
            if (byType.get(type).size() < 20) { // 每类最多20个
                Map<String, Object> item = new HashMap<>();
                item.put("word", info.word);
                item.put("frequency", info.frequency);
                item.put("score", info.frequency * info.weight);
                byType.get(type).add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("words", byType);
        result.put("counts", typeCounts);
        result.put("scores", typeScores);
        return result;
    }

    /**
     * 按行业分组统计
     */
    private Map<String, Object> groupByIndustry(List<WordInfo> wordInfoList) {
        Map<String, List<Map<String, Object>>> byIndustry = new HashMap<>();
        Map<String, Integer> industryCounts = new HashMap<>();
        Map<String, Double> industryScores = new HashMap<>();

        for (WordInfo info : wordInfoList) {
            if (info.industry == null || info.industry.isEmpty()) continue;

            String industry = info.industry;
            industryCounts.put(industry, industryCounts.getOrDefault(industry, 0) + info.frequency);
            industryScores.put(industry, industryScores.getOrDefault(industry, 0.0) + info.frequency * info.weight);

            byIndustry.computeIfAbsent(industry, k -> new ArrayList<>());
            if (byIndustry.get(industry).size() < 10) {
                Map<String, Object> item = new HashMap<>();
                item.put("word", info.word);
                item.put("frequency", info.frequency);
                item.put("score", info.frequency * info.weight);
                byIndustry.get(industry).add(item);
            }
        }

        // 按热度排序行业
        List<Map.Entry<String, Double>> sortedIndustries = new ArrayList<>(industryScores.entrySet());
        sortedIndustries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> industryRank = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedIndustries) {
            Map<String, Object> item = new HashMap<>();
            item.put("industry", entry.getKey());
            item.put("score", entry.getValue());
            item.put("count", industryCounts.get(entry.getKey()));
            item.put("words", byIndustry.get(entry.getKey()));
            industryRank.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rank", industryRank);
        result.put("counts", industryCounts);
        result.put("scores", industryScores);
        return result;
    }

    /**
     * 保存热词到数据库（增强版）
     */
    private void saveHotWordsToDbEnhanced(List<WordInfo> wordInfoList, String source) {
        try {
            LocalDate today = LocalDate.now();
            int currentHour = LocalDateTime.now().getHour();
            String actualSource = (source == null || source.trim().isEmpty() || "全部".equals(source)) ? "全部" : source;
            LocalDateTime now = LocalDateTime.now();

            int count = 0;
            for (WordInfo info : wordInfoList) {
                if (count >= 100) break; // 最多保存100个

                // 只保存有意义的词
                if (info.frequency < 2 && info.weight < 2.0) continue;

                LambdaQueryWrapper<HotWord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(HotWord::getWord, info.word)
                       .eq(HotWord::getSource, actualSource)
                       .eq(HotWord::getDataDate, today)
                       .eq(HotWord::getDataHour, currentHour);

                HotWord existing = hotWordDao.selectOne(wrapper);

                if (existing != null) {
                    existing.setFrequency(info.frequency);
                    existing.setWeight(BigDecimal.valueOf(info.weight));
                    existing.setScore(BigDecimal.valueOf(info.frequency * info.weight));
                    existing.setWordType(info.wordType);
                    existing.setSentimentScore(BigDecimal.valueOf(info.sentimentValue));
                    existing.setIndustry(info.industry);
                    existing.setLastAppearTime(now);
                    existing.setAppearCount((existing.getAppearCount() != null ? existing.getAppearCount() : 0) + 1);
                    existing.setUpdatedAt(now);
                    hotWordDao.updateById(existing);
                } else {
                    HotWord hotWord = new HotWord();
                    hotWord.setWord(info.word);
                    hotWord.setFrequency(info.frequency);
                    hotWord.setWeight(BigDecimal.valueOf(info.weight));
                    hotWord.setScore(BigDecimal.valueOf(info.frequency * info.weight));
                    hotWord.setSource(actualSource);
                    hotWord.setDataDate(today);
                    hotWord.setDataHour(currentHour);
                    hotWord.setWordType(info.wordType);
                    hotWord.setSentimentScore(BigDecimal.valueOf(info.sentimentValue));
                    hotWord.setIndustry(info.industry);
                    hotWord.setFirstAppearTime(now);
                    hotWord.setLastAppearTime(now);
                    hotWord.setAppearCount(1);
                    hotWord.setCreatedAt(now);
                    hotWord.setUpdatedAt(now);
                    hotWordDao.insert(hotWord);
                }
                count++;
            }
            System.out.println("保存热词到数据库成功，共 " + count + " 条，来源: " + actualSource);
        } catch (Exception e) {
            System.err.println("保存热词到数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 自动学习新词汇
     */
    private void learnNewWords(List<WordInfo> wordInfoList) {
        try {
            LocalDate today = LocalDate.now();
            int newWordCount = 0;

            for (WordInfo info : wordInfoList) {
                // 只学习高频新词（频次>=5）
                if (!info.isNew || info.frequency < 5) continue;

                // 检查是否已存在
                LambdaQueryWrapper<WordDictionary> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WordDictionary::getWord, info.word);
                WordDictionary existing = wordDictionaryDao.selectOne(wrapper);

                if (existing == null) {
                    WordDictionary newWord = new WordDictionary();
                    newWord.setWord(info.word);
                    newWord.setWordType(info.wordType);
                    newWord.setBaseWeight(BigDecimal.valueOf(1.5)); // 新词默认权重1.5
                    newWord.setSentimentValue(BigDecimal.ZERO);
                    newWord.setIsSystem(false);
                    newWord.setFrequencyTotal(info.frequency);
                    newWord.setLastSeenDate(today);
                    newWord.setCreatedAt(LocalDateTime.now());
                    newWord.setUpdatedAt(LocalDateTime.now());
                    wordDictionaryDao.insert(newWord);

                    // 更新缓存
                    wordDictCache.put(info.word, newWord);
                    newWordCount++;
                } else {
                    // 更新已有词汇的统计
                    existing.setFrequencyTotal(existing.getFrequencyTotal() + info.frequency);
                    existing.setLastSeenDate(today);
                    existing.setUpdatedAt(LocalDateTime.now());
                    wordDictionaryDao.updateById(existing);
                }
            }

            if (newWordCount > 0) {
                System.out.println("自动学习新词汇: " + newWordCount + " 个");
            }
        } catch (Exception e) {
            System.err.println("学习新词汇失败: " + e.getMessage());
        }
    }

    /**
     * 分词方法
     */
    private List<String> splitWords(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.isEmpty()) return words;

        // 清理文本
        String cleanText = text.replaceAll("[\\p{P}\\p{S}\\s]+", " ");
        String[] segments = cleanText.split("\\s+");

        for (String segment : segments) {
            if (segment.length() < 2) continue;

            // 提取2-4字词
            for (int i = 0; i < segment.length(); i++) {
                for (int len = 2; len <= 4 && i + len <= segment.length(); len++) {
                    String word = segment.substring(i, i + len);
                    if (isValidWord(word)) {
                        words.add(word);
                    }
                }
            }
        }
        return words;
    }

    private boolean isValidWord(String word) {
        if (word == null || word.length() < 2) return false;
        for (char c : word.toCharArray()) {
            if (Character.UnicodeScript.of(c) != Character.UnicodeScript.HAN) {
                return false;
            }
        }
        return true;
    }

    private int getSentimentCategory(double score) {
        if (score > 10.0) return 0;      // 强烈看涨
        if (score > 5.0) return 1;       // 看涨
        if (score > 0) return 2;         // 偏多
        if (score > -5.0) return 3;      // 中性
        if (score > -10.0) return 4;     // 偏空
        return 5;                         // 看跌
    }

    private String getSentimentDescription(int category) {
        switch (category) {
            case 0: return "强烈看涨";
            case 1: return "看涨";
            case 2: return "偏多";
            case 3: return "中性";
            case 4: return "偏空";
            case 5: return "看跌";
            default: return "未知";
        }
    }

    private Map<String, Object> buildDefaultResult() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> defaultSentiment = new HashMap<>();
        defaultSentiment.put("Score", 0.0);
        defaultSentiment.put("Category", 3);
        defaultSentiment.put("PositiveCount", 0);
        defaultSentiment.put("NegativeCount", 0);
        defaultSentiment.put("Description", "中性");
        result.put("result", defaultSentiment);
        result.put("frequencies", new ArrayList<>());
        result.put("byType", new HashMap<>());
        result.put("byIndustry", new HashMap<>());
        return result;
    }
}
