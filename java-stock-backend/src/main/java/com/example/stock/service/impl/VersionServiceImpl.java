package com.example.stock.service.impl;

import com.example.stock.service.VersionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VersionServiceImpl implements VersionService {

    @Override
    public Map<String, Object> getVersionInfo() {
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("version", "1.0.0");
        versionInfo.put("buildTime", "2023-12-23");
        versionInfo.put("platform", "Java");
        versionInfo.put("backend", "Spring Boot");
        return versionInfo;
    }
}