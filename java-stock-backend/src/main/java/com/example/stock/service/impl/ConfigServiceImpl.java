package com.example.stock.service.impl;

import com.example.stock.service.ConfigService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Override
    public Map<String, Object> exportConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enableNews", true);
        config.put("enableFund", false);
        config.put("enableAgent", false);
        config.put("darkTheme", false);
        config.put("tushareToken", "");
        config.put("aiConfigs", new Object[]{});
        config.put("groups", new Object[]{});
        return config;
    }

    @Override
    public String updateConfig(Map<String, Object> config) {
        // 模拟更新配置
        System.out.println("更新配置: " + config);
        return "配置更新成功";
    }
}