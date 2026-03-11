package com.example.stock.service;

import java.util.Map;

public interface ConfigService {
    /**
     * 导出配置
     *
     * @return 配置数据
     */
    Map<String, Object> exportConfig();

    /**
     * 更新配置
     *
     * @param config 配置数据
     * @return 操作结果
     */
    String updateConfig(Map<String, Object> config);
}