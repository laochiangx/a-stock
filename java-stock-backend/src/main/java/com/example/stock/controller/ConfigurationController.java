package com.example.stock.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置控制器
 */
@RestController
@RequestMapping("/api")
public class ConfigurationController {

    @Value("${tushare.token:}")
    private String tushareToken;

    /**
     * 获取应用配置
     *
     * @return 配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enableNews", true);
        config.put("enableFund", false);
        config.put("enableAgent", false);
        config.put("darkTheme", false);
        config.put("tushareToken", tushareToken != null && !tushareToken.isEmpty());
        // 添加前端期望的字段
        config.put("openAiEnable", true);  // 启用AI功能
        config.put("httpProxyEnabled", false);  // HTTP代理默认关闭
        config.put("enableDanmu", false);  // 弹幕功能
        config.put("dingPushEnable", false);  // 钉钉推送
        config.put("dingRobot", "");  // 钉钉机器人地址
        config.put("updateBasicInfoOnStart", false);  // 启动时更新基本信息
        config.put("refreshInterval", 1);  // 刷新间隔
        config.put("enablePushNews", false);  // 推送新闻
        config.put("enableOnlyPushRedNews", false);  // 仅推送红色新闻
        config.put("browserPath", "");  // 浏览器路径
        config.put("enableNews", true);  // 新闻功能
        config.put("enableFund", false);  // 基金功能
        config.put("ID", 1);  // 配置ID
        return config;
    }


}