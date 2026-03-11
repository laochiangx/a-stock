package com.example.stock.service;

import java.util.Map;

public interface UtilsService {
    /**
     * 保存为 Markdown
     *
     * @param content 内容
     * @return 操作结果
     */
    String saveAsMarkdown(String content);

    /**
     * 保存图片
     *
     * @param data 数据
     * @return 操作结果
     */
    String saveImage(Map<String, Object> data);

    /**
     * 保存 Word 文件
     *
     * @param content 内容
     * @return 操作结果
     */
    String saveWordFile(String content);

    /**
     * 分享分析
     *
     * @param data 数据
     * @return 操作结果
     */
    String shareAnalysis(Map<String, Object> data);

    /**
     * 发送钉钉消息
     *
     * @param type 类型
     * @param content 内容
     * @return 操作结果
     */
    String sendDingDingMessageByType(String type, String content);
}