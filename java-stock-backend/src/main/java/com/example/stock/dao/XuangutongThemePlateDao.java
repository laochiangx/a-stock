package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.XuangutongThemePlate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface XuangutongThemePlateDao extends BaseMapper<XuangutongThemePlate> {
    
    @Select("SELECT DISTINCT plate_id, plate_name, plate_description FROM xuangutong_theme_plate WHERE data_date = #{date} AND deleted = 0")
    List<XuangutongThemePlate> findPlatesByDate(@Param("date") LocalDate date);
    
    @Select("SELECT COUNT(*) FROM xuangutong_theme_plate WHERE data_date = #{date} AND deleted = 0")
    int countByDate(@Param("date") LocalDate date);
    
    @Select("SELECT * FROM xuangutong_theme_plate WHERE data_date = #{date} AND plate_id = #{plateId} AND deleted = 0")
    List<XuangutongThemePlate> findStocksByDateAndPlateId(@Param("date") LocalDate date, @Param("plateId") String plateId);
}
