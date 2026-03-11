package com.example.stock.controller;

import com.example.stock.service.FundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 基金控制器
 */
@RestController
@RequestMapping("/api")
public class FundController {

    @Autowired
    private FundService fundService;

    /**
     * 关注基金
     *
     * @param fundCode 基金代码
     * @return 操作结果
     */
    @PostMapping("/fund/follow")
    public String followFund(@RequestParam String fundCode) {
        return fundService.followFund(fundCode);
    }

    /**
     * 取消关注基金
     *
     * @param fundCode 基金代码
     * @return 操作结果
     */
    @DeleteMapping("/fund/unfollow/{fundCode}")
    public String unfollowFund(@PathVariable String fundCode) {
        return fundService.unfollowFund(fundCode);
    }

    /**
     * 获取已关注基金列表
     *
     * @return 基金列表
     */
    @GetMapping("/fund/followed")
    public List<Map<String, Object>> getFollowedFund() {
        return fundService.getFollowedFund();
    }

    /**
     * 获取基金列表
     *
     * @param query 查询条件
     * @return 基金列表
     */
    @GetMapping("/funds")
    public List<Map<String, Object>> getFundList(@RequestParam(required = false) String query) {
        return fundService.getFundList(query);
    }
}