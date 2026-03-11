package com.example.stock.controller;

import com.example.stock.dto.MarketNewsDTO;
import com.example.stock.service.MarketNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 市场资讯控制器
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    @Autowired
    private MarketNewsService marketNewsService;

    /**
     * 获取电报列表
     *
     * @param source 资讯来源 (可选)
     * @return 电报列表
     */
    @GetMapping("/telegraph")
    public List<MarketNewsDTO> getTelegraphList(@RequestParam(required = false) String source) {
        return marketNewsService.getTelegraphList(source);
    }

    /**
     * 获取新浪财经资讯列表
     *
     * @return 新浪财经资讯列表
     */
    @GetMapping("/sina-news")
    public List<MarketNewsDTO> getSinaNews() {
        return marketNewsService.getTelegraphList("sina");
    }
}