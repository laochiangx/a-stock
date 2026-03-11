package com.example.stock.controller;

import com.example.stock.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 新闻控制器
 */
@RestController
@RequestMapping("/api")
public class NewsController {

    @Autowired
    private NewsService newsService;

    /**
     * 保存 Word 文件
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/utils/save-word")
    public String saveWordFile(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        return newsService.saveWordFile(content);
    }

    /**
     * 分享分析
     *
     * @param data 数据
     * @return 操作结果
     */
    @PostMapping("/utils/share")
    public String shareAnalysis(@RequestBody Map<String, Object> data) {
        return newsService.shareAnalysis(data);
    }

    /**
     * 发送钉钉消息
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/utils/dingding")
    public String sendDingDingMessage(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String content = (String) request.get("content");
        return newsService.sendDingDingMessageByType(type, content);
    }
}