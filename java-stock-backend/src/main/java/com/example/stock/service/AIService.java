package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    /**
     * 获取AI配置
     *
     * @return AI配置列表
     */
    List<Map<String, Object>> getAiConfigs();

    /**
     * 获取提示模板
     *
     * @param type 类型
     * @param content 内容
     * @return 提示模板列表
     */
    List<Map<String, Object>> getPromptTemplates(String type, String content);

    /**
     * 保存AI响应结果
     *
     * @param request 请求参数
     * @return 操作结果
     */
    String saveAIResponseResult(Map<String, Object> request);

    /**
     * 获取AI响应结果
     *
     * @param params 参数
     * @return AI响应结果
     */
    Map<String, Object> getAIResponseResult(Map<String, String> params);

    /**
     * 与AI代理聊天
     *
     * @param question 问题
     * @param configId 配置ID
     * @param params 参数
     * @return 聊天结果
     */
    Map<String, Object> chatWithAgent(String question, Integer configId, Map<String, Object> params);

    /**
     * 总结股票新闻
     *
     * @param request 请求参数
     * @return 总结结果
     */
    Map<String, Object> summaryStockNews(Map<String, Object> request);

    /**
     * 情感分析带权重词频统计
     *
     * @param text 要分析的文本
     * @return 情感分析结果
     */
    Map<String, Object> analyzeSentimentWithFreqWeight(String text, String source);
}