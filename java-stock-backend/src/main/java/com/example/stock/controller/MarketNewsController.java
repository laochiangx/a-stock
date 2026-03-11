package com.example.stock.controller;

import com.example.stock.dto.MarketNewsDTO;
import com.example.stock.service.MarketNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 市场新闻控制器
 */
@RestController
@RequestMapping("/api")
public class MarketNewsController {

    @Autowired
    private MarketNewsService marketNewsService;

    /**
     * 获取电报列表
     *
     * @param source 来源
     * @return 电报列表
     */
    @GetMapping("/market-news/telegraph")
    public List<MarketNewsDTO> getTelegraphList(@RequestParam(required = false) String source) {
        try {
            return marketNewsService.getTelegraphList(source);
        } catch (Exception e) {
            System.err.println("Error in getTelegraphList: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }

    /**
     * 刷新电报列表
     *
     * @param source 来源
     * @return 刷新结果
     */
    @GetMapping("/market-news/telegraph/refresh")
    public List<MarketNewsDTO> refreshTelegraphList(@RequestParam(required = false) String source) {
        try {
            return marketNewsService.refreshTelegraphList(source);
        } catch (Exception e) {
            System.err.println("Error in refreshTelegraphList: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }

    /**
     * 总结股票新闻
     *
     * @param request 请求参数
     * @return 总结结果
     */
    @PostMapping("/news/summary")
    public Map<String, Object> summaryStockNews(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        Integer configId = request.get("configId") != null ? 
            Integer.valueOf(request.get("configId").toString()) : null;
        String promptId = (String) request.get("promptId");
        Boolean enableTools = (Boolean) request.get("enableTools");
        String thinkingMode = (String) request.get("thinkingMode");
        
        return marketNewsService.summaryStockNews(question, configId, promptId, enableTools, thinkingMode);
    }
    
    /**
     * 获取个股资金流向排名
     *
     * @param sort 排序方式 (netamount, r0_net, r3_net等)
     * @return 个股资金流向排名列表
     */
    @GetMapping("/market-news/money-rank")
    public List<Map<String, Object>> getMoneyRankSina(@RequestParam(required = false, defaultValue = "netamount") String sort) {
        try {
            return marketNewsService.getMoneyRankSina(sort);
        } catch (Exception e) {
            System.err.println("Error in getMoneyRankSina: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}