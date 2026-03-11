package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface StockDataService {

    /**
     * 获取龙虎榜数据
     */
    Map<String, Object> getDragonTigerList(String date);

    /**
     * 获取个股研报
     */
    List<Map<String, Object>> getStockResearchReport(String stockCode);

    /**
     * 获取行业研究报告
     */
    List<Map<String, Object>> getIndustryResearchReport(String industryCode);

    /**
     * 获取个股资金流向
     */
    List<Map<String, Object>> getStockMoneyFlow(String stockCode);

    /**
     * 获取公司公告
     */
    List<Map<String, Object>> getCompanyNotice(String stockCode);
    
    /**
     * 获取个股资金趋势
     */
    List<Map<String, Object>> getStockMoneyTrendByDay(String stockCode, int days);
    
    /**
     * 获取个股资金流向排名
     */
    List<Map<String, Object>> getMoneyRankSina(String sort);
    
    /**
     * 获取行业资金排名
     * @param fenlei 分类: 0-行业, 1-概念, 2-地域
     * @param sort 排序字段
     */
    List<Map<String, Object>> getIndustryMoneyRankSina(String fenlei, String sort);
    
    /**
     * 获取股吧热门话题
     * @param size 数量
     */
    List<Map<String, Object>> getHotTopic(int size);
    
    /**
     * 获取热门选股策略
     */
    Map<String, Object> getHotStrategy();
    
    /**
     * 智能选股
     * @param words 选股条件
     * @param pageSize 每页数量
     */
    Map<String, Object> searchStock(String words, int pageSize);
}