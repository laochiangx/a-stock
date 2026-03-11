package com.example.stock.service;

import java.util.Map;

public interface PromptService {
    /**
     * 添加提示词
     *
     * @param prompt 提示词
     * @return 操作结果
     */
    String addPrompt(Map<String, String> prompt);

    /**
     * 删除提示词
     *
     * @param promptId 提示词ID
     * @return 操作结果
     */
    String delPrompt(String promptId);
}