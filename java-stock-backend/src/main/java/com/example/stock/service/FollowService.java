package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface FollowService {
    /**
     * 获取关注列表
     *
     * @param groupId 分组ID
     * @return 关注列表
     */
    List<Map<String, Object>> getFollowList(Integer groupId);

    /**
     * 关注股票
     *
     * @param stockCode 股票代码
     * @return 操作结果
     */
    String follow(String stockCode);

    /**
     * 取消关注股票
     *
     * @param stockCode 股票代码
     * @return 操作结果
     */
    String unfollow(String stockCode);

    /**
     * 设置股票排序
     *
     * @param stockCode 股票代码
     * @param sort 排序值
     * @return 操作结果
     */
    String setStockSort(String stockCode, Integer sort);

    /**
     * 设置股票价格提醒
     *
     * @param stockCode 股票代码
     * @param percent 提醒百分比
     * @param alarmPrice 报警价格
     * @return 操作结果
     */
    String setAlarmChangePercent(String stockCode, Double percent, Double alarmPrice);

    /**
     * 设置成本价和数量
     *
     * @param stockCode 股票代码
     * @param costPrice 成本价
     * @param volume 数量
     * @return 操作结果
     */
    String setCostPriceAndVolume(String stockCode, Double costPrice, Integer volume);

    /**
     * 设置AI定时任务
     *
     * @param stockCode 股票代码
     * @param cron Cron表达式
     * @return 操作结果
     */
    String setStockAICron(String stockCode, String cron);

    /**
     * 添加股票到分组
     *
     * @param stockCode 股票代码
     * @param groupId 分组ID
     * @return 操作结果
     */
    String addStockGroup(String stockCode, Integer groupId);

    /**
     * 从分组移除股票
     *
     * @param stockCode 股票代码
     * @param groupId 分组ID
     * @return 操作结果
     */
    String removeStockGroup(String stockCode, Integer groupId);
}