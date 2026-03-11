package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.ThemePlateData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ThemePlateDataDao extends BaseMapper<ThemePlateData> {
    
    @Select("SELECT * FROM theme_plate_data WHERE data_date = #{date} ORDER BY plate_name, enter_time")
    List<ThemePlateData> findByDate(@Param("date") LocalDate date);
    
    @Select("SELECT * FROM theme_plate_data WHERE data_date = #{date} AND plate_name = #{plateName} ORDER BY enter_time")
    List<ThemePlateData> findByDateAndPlate(@Param("date") LocalDate date, @Param("plateName") String plateName);

    @Select("SELECT * FROM theme_plate_data WHERE data_date = #{date} AND stock_code = #{stockCode} ORDER BY plate_name, enter_time")
    List<ThemePlateData> findByDateAndStockCode(@Param("date") LocalDate date, @Param("stockCode") String stockCode);
    
    @Select("SELECT COUNT(*) FROM theme_plate_data WHERE data_date = #{date}")
    int countByDate(@Param("date") LocalDate date);
}
