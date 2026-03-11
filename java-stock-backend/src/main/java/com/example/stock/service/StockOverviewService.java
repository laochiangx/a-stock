package com.example.stock.service;

import java.util.Map;

public interface StockOverviewService {

    Map<String, Object> listByThsHotRank(String date, String stockType, String listType);

    Map<String, Object> getDetail(String date, String stockCode, String stockType, String thsListType);
}
