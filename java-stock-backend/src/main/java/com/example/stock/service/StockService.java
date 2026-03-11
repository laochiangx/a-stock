package com.example.stock.service;

import com.example.stock.dto.StockDTO;

import java.util.List;
import java.util.Map;

public interface StockService {
    /**
     * 获取股票列表
     *
     * @param query 查询条件
     * @return 股票列表
     */
    List<StockDTO> getStockList(String query);

    /**
     * 获取股票实时数据
     *
     * @param stockCode 股票代码
     * @return 股票实时数据
     */
    Map<String, Object> getStockRealTimeData(String stockCode);

    /**
     * 获取K线数据
     *
     * @param stockCode 股票代码
     * @param name 股票名称
     * @param days 天数
     * @return K线数据
     */
    List<Map<String, Object>> getStockKLineData(String stockCode, String name, Integer days);

    /**
     * 获取分时数据
     *
     * @param stockCode 股票代码
     * @param name 股票名称
     * @return 分时数据
     */
    Map<String, Object> getStockMinuteData(String stockCode, String name);
}