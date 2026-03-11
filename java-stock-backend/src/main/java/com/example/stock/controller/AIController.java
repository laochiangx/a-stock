package com.example.stock.controller;

import com.example.stock.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI控制器
 */
@RestController
@RequestMapping("/api")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 获取AI配置
     *
     * @return AI配置列表
     */
    @GetMapping("/ai/configs")
    public List<Map<String, Object>> getAiConfigs() {
        return aiService.getAiConfigs();
    }

    /**
     * 获取提示模板
     *
     * @param type 类型
     * @param content 内容
     * @return 提示模板列表
     */
    @GetMapping("/ai/templates")
    public List<Map<String, Object>> getPromptTemplates(@RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String content) {
        return aiService.getPromptTemplates(type, content);
    }

    /**
     * 保存AI响应结果
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/ai/results")
    public String saveAIResponseResult(@RequestBody Map<String, Object> request) {
        return aiService.saveAIResponseResult(request);
    }

    /**
     * 获取AI响应结果
     *
     * @param params 参数
     * @return AI响应结果
     */
    @GetMapping("/ai/results")
    public Map<String, Object> getAIResponseResult(@RequestParam Map<String, String> params) {
        return aiService.getAIResponseResult(params);
    }

    /**
     * 与AI代理聊天
     *
     * @param request 请求参数
     * @return 聊天结果
     */
    @PostMapping("/ai/chat")
    public Map<String, Object> chatWithAgent(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        Integer configId = request.get("configId") != null ? 
            Integer.valueOf(request.get("configId").toString()) : null;
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        return aiService.chatWithAgent(question, configId, params);
    }

    /**
     * 情感分析带权重词频统计
     *
     * @param text 要分析的文本
     * @return 情感分析结果
     */
    @PostMapping("/ai/sentiment-analysis")
    public Map<String, Object> analyzeSentimentWithFreqWeight(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        String source = request.getOrDefault("source", "");
        return aiService.analyzeSentimentWithFreqWeight(text, source);
    }
}