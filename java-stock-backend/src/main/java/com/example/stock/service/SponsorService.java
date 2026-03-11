package com.example.stock.service;

import java.util.Map;

public interface SponsorService {
    /**
     * 获取赞助信息
     *
     * @return 赞助信息
     */
    Map<String, Object> getSponsorInfo();

    /**
     * 检查赞助码
     *
     * @param sponsorCode 赞助码
     * @return 检查结果
     */
    Map<String, Object> checkSponsorCode(String sponsorCode);
}