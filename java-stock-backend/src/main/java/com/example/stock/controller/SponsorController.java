package com.example.stock.controller;

import com.example.stock.service.SponsorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 赞助控制器
 */
@RestController
@RequestMapping("/api")
public class SponsorController {

    @Autowired
    private SponsorService sponsorService;

    /**
     * 获取赞助信息
     *
     * @return 赞助信息
     */
    @GetMapping("/sponsor/info")
    public Map<String, Object> getSponsorInfo() {
        return sponsorService.getSponsorInfo();
    }

    /**
     * 检查赞助码
     *
     * @param request 请求参数
     * @return 检查结果
     */
    @PostMapping("/sponsor/check")
    public Map<String, Object> checkSponsorCode(@RequestBody Map<String, String> request) {
        String sponsorCode = request.get("sponsorCode");
        return sponsorService.checkSponsorCode(sponsorCode);
    }
}