package com.example.stock.controller;

import com.example.stock.service.PopularityRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PopularityRankController {

    @Autowired
    private PopularityRankService popularityRankService;

    @GetMapping("/popularity-rank")
    public Map<String, Object> getPopularityRank(@RequestParam(required = false) Integer limit) {
        return popularityRankService.getPopularityRank(limit);
    }
}
