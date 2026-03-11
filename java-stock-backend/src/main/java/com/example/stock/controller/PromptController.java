package com.example.stock.controller;

import com.example.stock.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提示词控制器
 */
@RestController
@RequestMapping("/api")
public class PromptController {

    @Autowired
    private PromptService promptService;

    /**
     * 添加提示词
     *
     * @param prompt 提示词
     * @return 操作结果
     */
    @PostMapping("/prompt")
    public String addPrompt(@RequestBody Map<String, String> prompt) {
        return promptService.addPrompt(prompt);
    }

    /**
     * 删除提示词
     *
     * @param promptId 提示词ID
     * @return 操作结果
     */
    @DeleteMapping("/prompt/{promptId}")
    public String delPrompt(@PathVariable String promptId) {
        return promptService.delPrompt(promptId);
    }
}