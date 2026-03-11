package com.example.stock.service.impl;

import com.example.stock.service.SponsorService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SponsorServiceImpl implements SponsorService {

    @Override
    public Map<String, Object> getSponsorInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("isSponsor", false);
        result.put("sponsorCode", "");
        result.put("sponsorExpireTime", "");
        result.put("sponsorFeatures", new String[]{});
        return result;
    }

    @Override
    public Map<String, Object> checkSponsorCode(String sponsorCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "赞助码无效");
        return result;
    }
}