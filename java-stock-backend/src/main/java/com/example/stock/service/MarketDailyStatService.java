package com.example.stock.service;

import java.util.Map;

public interface MarketDailyStatService {

    Map<String, Object> syncDailyStats(String date, Boolean force);

    Map<String, Object> listDailyStats(String date, Integer days);
}
