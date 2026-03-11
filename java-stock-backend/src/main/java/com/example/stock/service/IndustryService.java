package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface IndustryService {
    /**
     * 获取行业排名
     *
     * @param sort 排序方式
     * @param limit 限制数量
     * @return 行业排名列表
     */
    List<Map<String, Object>> getIndustryRank(String sort, Integer limit);
}