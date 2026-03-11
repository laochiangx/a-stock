package com.example.stock.service;

import java.util.Map;

public interface TdxLbttService {

    Map<String, Object> sync(String startDate, String endDate, Boolean force);

    Map<String, Object> query(String startDate, String endDate, Integer days);
}
