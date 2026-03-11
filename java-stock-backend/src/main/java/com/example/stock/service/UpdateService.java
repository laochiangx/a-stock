package com.example.stock.service;

import java.util.Map;

public interface UpdateService {
    /**
     * 检查更新
     *
     * @param params 参数
     * @return 更新信息
     */
    Map<String, Object> checkUpdate(Map<String, String> params);
}