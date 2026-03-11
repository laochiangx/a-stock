package com.example.stock.service.impl;

import com.example.stock.service.NewsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NewsServiceImpl implements NewsService {

    @Override
    public String saveWordFile(String content) {
        // 模拟保存Word文件
        System.out.println("保存Word文件: " + content);
        return "Word文件保存成功";
    }

    @Override
    public String shareAnalysis(Map<String, Object> data) {
        // 模拟分享分析
        System.out.println("分享分析: " + data);
        return "分析分享成功";
    }

    @Override
    public String sendDingDingMessageByType(String type, String content) {
        // 模拟发送钉钉消息
        System.out.println("发送钉钉消息 - 类型: " + type + ", 内容: " + content);
        return "钉钉消息发送成功";
    }
}