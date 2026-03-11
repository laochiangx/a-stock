package com.example.stock.service.impl;

import com.example.stock.service.PromptService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PromptServiceImpl implements PromptService {

    // 模拟提示词存储
    private static final Map<String, Map<String, String>> prompts = new HashMap<>();

    static {
        // 初始化一些提示词
        Map<String, String> prompt1 = new HashMap<>();
        prompt1.put("ID", "1");
        prompt1.put("name", "股票分析模板");
        prompt1.put("content", "请分析这只股票的基本面和技术面");
        prompt1.put("type", "模型系统Prompt");
        prompts.put("1", prompt1);

        Map<String, String> prompt2 = new HashMap<>();
        prompt2.put("ID", "2");
        prompt2.put("name", "基金分析模板");
        prompt2.put("content", "请分析这只基金的基本面和收益情况");
        prompt2.put("type", "模型系统Prompt");
        prompts.put("2", prompt2);
    }

    @Override
    public String addPrompt(Map<String, String> prompt) {
        String id = prompt.get("ID");
        if (id == null || id.isEmpty()) {
            // 生成新ID
            id = String.valueOf(prompts.size() + 1);
            prompt.put("ID", id);
        }
        prompts.put(id, prompt);
        return "提示词添加成功";
    }

    @Override
    public String delPrompt(String promptId) {
        if (prompts.remove(promptId) != null) {
            return "提示词删除成功";
        }
        return "提示词不存在";
    }
}