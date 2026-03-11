package com.example.stock.controller;

import com.example.stock.service.MarketDailyStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "*")
public class MarketDailyStatController {

    @Autowired
    private MarketDailyStatService marketDailyStatService;

    @GetMapping("/daily/sync")
    public Map<String, Object> sync(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Boolean force) {
        return marketDailyStatService.syncDailyStats(date, force);
    }

    @GetMapping("/daily/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer days) {
        return marketDailyStatService.listDailyStats(date, days);
    }
}
