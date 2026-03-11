package com.example.stock.controller;

import com.example.stock.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-data")
@CrossOrigin(origins = "*")
public class StockDataController {

    @Autowired
    private StockDataService stockDataService;

    /**
     * 获取龙虎榜数据
     */
    @GetMapping("/dragon-tiger")
    public Map<String, Object> getDragonTigerList(@RequestParam(required = false, defaultValue = "") String date) {
        return stockDataService.getDragonTigerList(date);
    }

    /**
     * 获取个股研报
     */
    @GetMapping("/research-report")
    public List<Map<String, Object>> getStockResearchReport(@RequestParam String stockCode) {
        return stockDataService.getStockResearchReport(stockCode);
    }

    /**
     * 获取行业研究报告
     */
    @GetMapping("/industry-research")
    public List<Map<String, Object>> getIndustryResearchReport(@RequestParam(required = false, defaultValue = "") String industryCode) {
        return stockDataService.getIndustryResearchReport(industryCode);
    }

    /**
     * 获取个股资金流向
     */
    @GetMapping("/money-flow")
    public List<Map<String, Object>> getStockMoneyFlow(@RequestParam String stockCode) {
        return stockDataService.getStockMoneyFlow(stockCode);
    }

    /**
     * 获取公司公告
     */
    @GetMapping("/company-notice")
    public List<Map<String, Object>> getCompanyNotice(@RequestParam String stockCode) {
        return stockDataService.getCompanyNotice(stockCode);
    }
    
    /**
     * 获取个股资金趋势
     */
    @GetMapping("/money-trend")
    public List<Map<String, Object>> getStockMoneyTrendByDay(@RequestParam String stockCode, @RequestParam(defaultValue = "14") int days) {
        return stockDataService.getStockMoneyTrendByDay(stockCode, days);
    }
    
    /**
     * 获取个股资金流向排名
     */
    @GetMapping("/money-rank")
    public List<Map<String, Object>> getMoneyRankSina(@RequestParam(required = false, defaultValue = "netamount") String sort) {
        return stockDataService.getMoneyRankSina(sort);
    }
    
    /**
     * 获取行业资金排名
     */
    @GetMapping("/industry-money-rank")
    public List<Map<String, Object>> getIndustryMoneyRankSina(
            @RequestParam(required = false, defaultValue = "0") String fenlei,
            @RequestParam(required = false, defaultValue = "netamount") String sort) {
        return stockDataService.getIndustryMoneyRankSina(fenlei, sort);
    }
    
    /**
     * 获取股吧热门话题
     */
    @GetMapping("/hot-topic")
    public List<Map<String, Object>> getHotTopic(
            @RequestParam(required = false, defaultValue = "10") int size) {
        return stockDataService.getHotTopic(size);
    }
    
    /**
     * 获取热门选股策略
     */
    @GetMapping("/hot-strategy")
    public Map<String, Object> getHotStrategy() {
        return stockDataService.getHotStrategy();
    }
    
    /**
     * 智能选股
     */
    @PostMapping("/search-stock")
    public Map<String, Object> searchStock(@RequestBody Map<String, Object> request) {
        String words = (String) request.get("words");
        Integer pageSize = (Integer) request.getOrDefault("pageSize", 50);
        return stockDataService.searchStock(words, pageSize);
    }
}