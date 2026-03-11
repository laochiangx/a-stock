package com.example.stock.service;

import com.example.stock.dto.MarketNewsDTO;

import java.util.List;
import java.util.Map;

public interface MarketNewsService {
    /**
     * 获取电报列表
     *
     * @param source 来源
     * @return 电报列表
     */
    List<MarketNewsDTO> getTelegraphList(String source);

    /**
     * 刷新电报列表
     *
     * @param source 来源
     * @return 刷新结果
     */
    List<MarketNewsDTO> refreshTelegraphList(String source);

    /**
     * 总结股票新闻
     *
     * @param question 问题
     * @param configId 配置ID
     * @param promptId 提示词ID
     * @param enableTools 启用工具
     * @param thinkingMode 思维模式
     * @return 总结结果
     */
    Map<String, Object> summaryStockNews(String question, Integer configId, String promptId, 
                                         Boolean enableTools, String thinkingMode);
    
    /**
     * 获取个股资金流向排名
     *
     * @param sort 排序方式 (netamount, r0_net, r3_net等)
     * @return 个股资金流向排名列表
     */
    List<Map<String, Object>> getMoneyRankSina(String sort);
}