package com.example.stock.service;

import java.util.Map;

/**
 * 题材板块服务接口
 * 获取选股通和东财的题材板块和个股数据
 */
public interface ThemePlateService {
    
    /**
     * 同步选股通题材板块数据到数据库
     * 首次进入页面时调用，从API获取数据并保存到数据库
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> syncData(String date);

    default Map<String, Object> syncData(String date, boolean force) {
        return syncData(date);
    }
    
    /**
     * 同步东财题材板块数据到数据库
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> syncDongcaiData(String date);

    default Map<String, Object> syncDongcaiData(String date, boolean force) {
        return syncDongcaiData(date);
    }
    
    /**
     * 获取选股通题材板块数据
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> getPlates(String date);
    
    /**
     * 获取选股通板块个股数据
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> getStocks(String date);
    
    /**
     * 获取完整的选股通题材板块数据（板块+个股，按板块分组）
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> getFullData(String date);

    /**
     * 获取东财题材板块列表
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> getDongcaiPlates(String date);

    /**
     * 获取东财题材板块个股列表
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     * @param plateId 板块ID
     */
    Map<String, Object> getDongcaiStocks(String date, Long plateId);

    /**
     * 获取完整的东财题材数据（板块+个股，按板块分组）
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    Map<String, Object> getDongcaiFullData(String date);

    Map<String, Object> syncWuyangData(String date);

    default Map<String, Object> syncWuyangData(String date, boolean force) {
        return syncWuyangData(date);
    }

    Map<String, Object> getWuyangSubjects(String date);

    Map<String, Object> getWuyangStocks(String date, String subjectName);

    Map<String, Object> getDongcaiHotThemes(Integer bdType);

    Map<String, Object> getDongcaiHotThemeStocks(String themeCode, Integer pageNum, Integer pageSize, Integer sort, String sortField);

    Map<String, Object> getDongcaiHotBoards(Integer boardType, Integer pageNumber, Integer pageSize);

    Map<String, Object> getDongcaiHotBoardStocks(String boardCode, Integer pn, Integer pz);
}
