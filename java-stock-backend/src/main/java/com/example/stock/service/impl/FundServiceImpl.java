package com.example.stock.service.impl;

import com.example.stock.service.FundService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FundServiceImpl implements FundService {

    // 模拟基金关注列表
    private static final Set<String> followedFunds = new HashSet<>();
    private static final List<Map<String, Object>> allFunds = new ArrayList<>();

    static {
        // 初始化一些基金数据
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> fund = new HashMap<>();
            fund.put("code", "00000" + i);
            fund.put("name", "基金" + i);
            fund.put("netEstimatedUnit", 1.0 + (Math.random() * 0.5));
            fund.put("netEstimatedRate", (Math.random() - 0.5) * 10);
            fund.put("netEstimatedUnitTime", new Date());
            allFunds.add(fund);
        }
    }

    @Override
    public String followFund(String fundCode) {
        followedFunds.add(fundCode);
        return "基金关注成功";
    }

    @Override
    public String unfollowFund(String fundCode) {
        followedFunds.remove(fundCode);
        return "基金取消关注成功";
    }

    @Override
    public List<Map<String, Object>> getFollowedFund() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String fundCode : followedFunds) {
            for (Map<String, Object> fund : allFunds) {
                if (fund.get("code").equals(fundCode)) {
                    result.add(fund);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getFundList(String query) {
        if (query == null || query.isEmpty()) {
            return allFunds;
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> fund : allFunds) {
            if (fund.get("name").toString().contains(query) || 
                fund.get("code").toString().contains(query)) {
                result.add(fund);
            }
        }
        return result;
    }
}