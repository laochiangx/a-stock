package com.example.stock.service.impl;

import com.example.stock.service.UtilsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UtilsServiceImpl implements UtilsService {

    @Override
    public String saveAsMarkdown(String content) {
        // 模拟保存为Markdown文件
        System.out.println("保存为Markdown文件: " + content);
        return "Markdown文件保存成功";
    }

    @Override
    public String saveImage(Map<String, Object> data) {
        // 模拟保存图片
        System.out.println("保存图片: " + data);
        return "图片保存成功";
    }

    @Override
    public String saveWordFile(String content) {
        // 模拟保存为Word文件
        System.out.println("保存为Word文件: " + content);
        return "Word文件保存成功";
    }

    @Override
    public String shareAnalysis(Map<String, Object> data) {
        // 模拟分享分析
        System.out.println("分享分析: " + data);
        return "分析已分享";
    }

    @Override
    public String sendDingDingMessageByType(String type, String content) {
        // 模拟发送钉钉消息
        System.out.println("发送钉钉消息 - 类型: " + type + ", 内容: " + content);
        return "钉钉消息发送成功";
    }
}