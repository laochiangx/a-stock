package com.example.stock.controller;

import com.example.stock.dto.StockDTO;
import com.example.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 股票控制器
 */
@RestController
@RequestMapping("/api")
public class StockController {

    @Autowired
    private StockService stockService;

    /**
     * 获取股票列表
     *
     * @param query 查询条件
     * @return 股票列表
     */
    @GetMapping("/stocks")
    public List<StockDTO> getStockList(@RequestParam(required = false) String query) {
        return stockService.getStockList(query);
    }

    /**
     * 获取股票实时数据
     *
     * @param stockCode 股票代码
     * @return 股票实时数据
     */
    @GetMapping("/stock/{stockCode}")
    public Map<String, Object> getStockRealTimeData(@PathVariable String stockCode) {
        return stockService.getStockRealTimeData(stockCode);
    }

    /**
     * 获取K线数据
     *
     * @param stockCode 股票代码
     * @param name 股票名称
     * @param days 天数
     * @return K线数据
     */
    @GetMapping("/stock/{stockCode}/kline")
    public List<Map<String, Object>> getStockKLine(@PathVariable String stockCode, 
                                                   @RequestParam(required = false) String name,
                                                   @RequestParam(defaultValue = "365") Integer days) {
        return stockService.getStockKLineData(stockCode, name, days);
    }

    /**
     * 获取分时数据
     *
     * @param stockCode 股票代码
     * @param name 股票名称
     * @return 分时数据
     */
    @GetMapping("/stock/{stockCode}/minute")
    public Map<String, Object> getStockMinuteData(@PathVariable String stockCode, 
                                                  @RequestParam(required = false) String name) {
        return stockService.getStockMinuteData(stockCode, name);
    }
}