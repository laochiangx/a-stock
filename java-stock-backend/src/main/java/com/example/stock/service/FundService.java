package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface FundService {
    /**
     * 关注基金
     *
     * @param fundCode 基金代码
     * @return 操作结果
     */
    String followFund(String fundCode);

    /**
     * 取消关注基金
     *
     * @param fundCode 基金代码
     * @return 操作结果
     */
    String unfollowFund(String fundCode);

    /**
     * 获取已关注基金列表
     *
     * @return 基金列表
     */
    List<Map<String, Object>> getFollowedFund();

    /**
     * 获取基金列表
     *
     * @param query 查询条件
     * @return 基金列表
     */
    List<Map<String, Object>> getFundList(String query);
}