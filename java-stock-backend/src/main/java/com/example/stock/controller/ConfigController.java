package com.example.stock.controller;

import com.example.stock.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 配置控制器
 */
@RestController
@RequestMapping("/api")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * 导出配置
     *
     * @return 配置数据
     */
    @GetMapping("/config/export")
    public Map<String, Object> exportConfig() {
        try {
            return configService.exportConfig();
        } catch (Exception e) {
            System.err.println("Error in exportConfig: " + e.getMessage());
            e.printStackTrace();
            // 返回默认配置而不是抛出异常
            Map<String, Object> defaultConfig = new java.util.HashMap<>();
            defaultConfig.put("enableNews", true);
            defaultConfig.put("darkTheme", false);
            defaultConfig.put("enableFund", false);
            defaultConfig.put("enableAgent", false);
            defaultConfig.put("tushareToken", false);
            return defaultConfig;
        }
    }

    /**
     * 更新配置
     *
     * @param config 配置数据
     * @return 操作结果
     */
    @PutMapping("/config")
    public String updateConfig(@RequestBody Map<String, Object> config) {
        try {
            return configService.updateConfig(config);
        } catch (Exception e) {
            System.err.println("Error in updateConfig: " + e.getMessage());
            e.printStackTrace();
            return "error"; // 返回错误信息而不是抛出异常
        }
    }
}