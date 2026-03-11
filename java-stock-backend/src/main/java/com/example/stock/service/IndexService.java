package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface IndexService {
    /**
     * 获取全球股指
     *
     * @return 全球股指列表，按地区分类
     */
    Map<String, List<Map<String, Object>>> getGlobalIndexes();
}