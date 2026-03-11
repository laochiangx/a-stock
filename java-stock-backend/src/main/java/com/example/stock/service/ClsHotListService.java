package com.example.stock.service;

import java.util.Map;

public interface ClsHotListService {

    Map<String, Object> syncDayHotList(String date, Boolean force, String listType);

    Map<String, Object> listDayHotList(String date, String listType);
}
