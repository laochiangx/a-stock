package com.example.stock.controller;

import com.example.stock.service.UtilsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工具控制器
 */
@RestController
@RequestMapping("/api")
public class UtilsController {

    @Autowired
    private UtilsService utilsService;

    /**
     * 保存为 Markdown
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/utils/save-markdown")
    public String saveAsMarkdown(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        return utilsService.saveAsMarkdown(content);
    }

    /**
     * 保存图片
     *
     * @param data 数据
     * @return 操作结果
     */
    @PostMapping("/utils/save-image")
    public String saveImage(@RequestBody Map<String, Object> data) {
        return utilsService.saveImage(data);
    }

    /**
     * 保存 Word 文件
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/utils/save-word-file")
    public String saveWordFile(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        return utilsService.saveWordFile(content);
    }

    /**
     * 分享分析
     *
     * @param data 数据
     * @return 操作结果
     */
    @PostMapping("/utils/share-analysis")
    public String shareAnalysis(@RequestBody Map<String, Object> data) {
        return utilsService.shareAnalysis(data);
    }

    /**
     * 发送钉钉消息
     *
     * @param data 数据
     * @return 操作结果
     */
    @PostMapping("/utils/send-dingding-message")
    public String sendDingDingMessage(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String content = (String) data.get("content");
        return utilsService.sendDingDingMessageByType(type, content);
    }
}