package com.example.stock.service;

import java.util.Map;

public interface ThsHotListService {

    Map<String, Object> syncDayHotList(String date, Boolean force, String stockType, String listType);

    Map<String, Object> listDayHotList(String date, String stockType, String listType);
}
