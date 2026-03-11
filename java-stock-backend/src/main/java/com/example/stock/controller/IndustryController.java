package com.example.stock.controller;

import com.example.stock.service.IndustryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/** 
 * 行业控制器
 */
@RestController
@RequestMapping("/api")
public class IndustryController {

    @Autowired
    private IndustryService industryService;

    /**
     * 获取行业排名
     *
     * @param sort 排序方式
     * @param limit 限制数量
     * @return 行业排名列表
     */
    @GetMapping("/industry/rank")
    public List<Map<String, Object>> getIndustryRank(@RequestParam(required = false) String sort, 
                                                     @RequestParam(required = false) Integer limit) {
        try {
            return industryService.getIndustryRank(sort, limit);
        } catch (Exception e) {
            System.err.println("Error in getIndustryRank: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }
}