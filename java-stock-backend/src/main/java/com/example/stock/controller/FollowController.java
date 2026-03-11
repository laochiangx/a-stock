package com.example.stock.controller;

import com.example.stock.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关注控制器
 */
@RestController
@RequestMapping("/api")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 获取关注列表
     *
     * @param groupId 分组ID
     * @return 关注列表
     */
    @GetMapping("/follow/list")
    public List<Map<String, Object>> getFollowList(@RequestParam(required = false) Integer groupId) {
        return followService.getFollowList(groupId);
    }

    /**
     * 关注股票
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/follow")
    public String follow(@RequestBody Map<String, String> request) {
        String stockCode = request.get("stockCode");
        return followService.follow(stockCode);
    }

    /**
     * 取消关注股票
     *
     * @param stockCode 股票代码
     * @return 操作结果
     */
    @DeleteMapping("/follow/{stockCode}")
    public String unfollow(@PathVariable String stockCode) {
        return followService.unfollow(stockCode);
    }

    /**
     * 设置股票排序
     *
     * @param stockCode 股票代码
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/follow/{stockCode}/sort")
    public String setStockSort(@PathVariable String stockCode, @RequestBody Map<String, Object> request) {
        Integer sort = request.get("sort") != null ? Integer.valueOf(request.get("sort").toString()) : 999;
        return followService.setStockSort(stockCode, sort);
    }

    /**
     * 设置股票价格提醒
     *
     * @param stockCode 股票代码
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/follow/{stockCode}/alarm")
    public String setAlarmChangePercent(@PathVariable String stockCode, @RequestBody Map<String, Object> request) {
        Double percent = request.get("percent") != null ? Double.valueOf(request.get("percent").toString()) : 0.0;
        Double alarmPrice = request.get("alarmPrice") != null ? Double.valueOf(request.get("alarmPrice").toString()) : 0.0;
        return followService.setAlarmChangePercent(stockCode, percent, alarmPrice);
    }

    /**
     * 设置成本价和数量
     *
     * @param stockCode 股票代码
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/follow/{stockCode}/cost")
    public String setCostPriceAndVolume(@PathVariable String stockCode, @RequestBody Map<String, Object> request) {
        Double costPrice = request.get("costPrice") != null ? Double.valueOf(request.get("costPrice").toString()) : 0.0;
        Integer volume = request.get("volume") != null ? Integer.valueOf(request.get("volume").toString()) : 0;
        return followService.setCostPriceAndVolume(stockCode, costPrice, volume);
    }

    /**
     * 设置AI定时任务
     *
     * @param stockCode 股票代码
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/follow/{stockCode}/cron")
    public String setStockAICron(@PathVariable String stockCode, @RequestBody Map<String, Object> request) {
        String cron = (String) request.get("cron");
        return followService.setStockAICron(stockCode, cron);
    }

    /**
     * 添加股票到分组
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/follow/stock-group")
    public String addStockGroup(@RequestBody Map<String, Object> request) {
        String stockCode = (String) request.get("stockCode");
        Integer groupId = (Integer) request.get("groupId");
        return followService.addStockGroup(stockCode, groupId);
    }

    /**
     * 从分组移除股票
     *
     * @param stockCode 股票代码
     * @param groupId 分组ID
     * @return 操作结果
     */
    @DeleteMapping("/follow/stock-group")
    public String removeStockGroup(@RequestParam String stockCode, @RequestParam Integer groupId) {
        return followService.removeStockGroup(stockCode, groupId);
    }
}