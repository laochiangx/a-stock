package com.example.stock.service;

import java.util.Map;

public interface PopularityRankService {
    Map<String, Object> getPopularityRank(Integer limit);
}
