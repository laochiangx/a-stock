package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.ThemePlateSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ThemePlateSummaryDao extends BaseMapper<ThemePlateSummary> {
    
    @Select("SELECT * FROM theme_plate_summary WHERE data_date = #{date} ORDER BY id")
    List<ThemePlateSummary> findByDate(@Param("date") LocalDate date);
    
    @Select("SELECT COUNT(*) FROM theme_plate_summary WHERE data_date = #{date}")
    int countByDate(@Param("date") LocalDate date);
}
