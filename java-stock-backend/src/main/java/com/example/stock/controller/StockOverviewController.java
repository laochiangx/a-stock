package com.example.stock.controller;

import com.example.stock.service.StockOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stock-overview")
@CrossOrigin(origins = "*")
public class StockOverviewController {

    @Autowired
    private StockOverviewService stockOverviewService;

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String stockType,
            @RequestParam(required = false) String listType
    ) {
        return stockOverviewService.listByThsHotRank(date, stockType, listType);
    }

    @GetMapping("/detail")
    public Map<String, Object> detail(
            @RequestParam(required = false) String date,
            @RequestParam String stockCode,
            @RequestParam(required = false) String stockType,
            @RequestParam(required = false) String thsListType
    ) {
        return stockOverviewService.getDetail(date, stockCode, stockType, thsListType);
    }
}
