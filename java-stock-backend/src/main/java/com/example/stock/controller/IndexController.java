package com.example.stock.controller;

import com.example.stock.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 指数控制器
 */
@RestController
@RequestMapping("/api")
public class IndexController {

    @Autowired
    private IndexService indexService;

    /**
     * 获取全球股指
     *
     * @return 全球股指列表，按地区分类
     */
    @GetMapping("/indexes/global")
    public Map<String, List<Map<String, Object>>> getGlobalIndexes() {
        try {
            return indexService.getGlobalIndexes();
        } catch (Exception e) {
            System.err.println("Error in getGlobalIndexes: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>(); // 返回空Map而不是抛出异常
        }
    }
}