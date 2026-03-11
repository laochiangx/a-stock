package com.example.stock.service;

import java.util.Map;

public interface DcHotListService {

    Map<String, Object> syncDayHotList(String date, Boolean force, String listType);

    Map<String, Object> listDayHotList(String date, String listType);
}
