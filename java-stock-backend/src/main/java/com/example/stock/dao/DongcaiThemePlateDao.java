package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.DongcaiThemePlate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DongcaiThemePlateDao extends BaseMapper<DongcaiThemePlate> {

    @Delete("DELETE FROM dongcai_theme_plate WHERE data_date = #{date}")
    void physicalDeleteByDate(@Param("date") LocalDate date);
    
    @Select("SELECT DISTINCT plate_id, plate_name, plate_description FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0")
    List<DongcaiThemePlate> findPlatesByDate(@Param("date") LocalDate date);

    @Select("SELECT plate_id, plate_name, plate_description, " +
            "SUM(CASE WHEN stock_code <> '' AND lianban IS NOT NULL AND TRIM(lianban) <> '' THEN 1 ELSE 0 END) AS ztCount " +
            "FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0 " +
            "GROUP BY plate_id, plate_name, plate_description " +
            "ORDER BY ztCount DESC, plate_name")
    List<DongcaiThemePlate> findPlatesWithZtCountByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(DISTINCT plate_id) FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0")
    int countDistinctPlatesByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(DISTINCT plate_id) FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0 AND stock_code <> ''")
    int countDistinctPlatesWithStocksByDate(@Param("date") LocalDate date);
    
    @Select("SELECT COUNT(*) FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0 AND stock_code <> ''")
    int countByDate(@Param("date") LocalDate date);
    
    @Select("SELECT * FROM dongcai_theme_plate WHERE data_date = #{date} AND plate_id = #{plateId} AND deleted = 0 AND stock_code <> ''")
    List<DongcaiThemePlate> findStocksByDateAndPlateId(@Param("date") LocalDate date, @Param("plateId") Long plateId);

    @Select("SELECT * FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0 AND stock_code = #{stockCode} AND stock_code <> '' ORDER BY plate_name")
    List<DongcaiThemePlate> findByDateAndStockCode(@Param("date") LocalDate date, @Param("stockCode") String stockCode);

    @Select("SELECT DISTINCT plate_name FROM dongcai_theme_plate WHERE data_date = #{date} AND deleted = 0 AND stock_code = #{stockCode} AND stock_code <> '' AND plate_name IS NOT NULL AND TRIM(plate_name) <> '' ORDER BY plate_name")
    List<String> findPlateNamesByDateAndStockCode(@Param("date") LocalDate date, @Param("stockCode") String stockCode);
}
