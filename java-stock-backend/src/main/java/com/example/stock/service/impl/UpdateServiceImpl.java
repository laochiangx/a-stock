package com.example.stock.service.impl;

import com.example.stock.service.UpdateService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UpdateServiceImpl implements UpdateService {

    @Override
    public Map<String, Object> checkUpdate(Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasUpdate", false);
        result.put("version", "1.0.0");
        result.put("downloadUrl", "");
        result.put("updateLog", "暂无更新");
        return result;
    }
}