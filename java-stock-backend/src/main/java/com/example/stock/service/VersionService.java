package com.example.stock.service;

import java.util.Map;

public interface VersionService {
    /**
     * 获取版本信息
     *
     * @return 版本信息
     */
    Map<String, Object> getVersionInfo();
}