package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.WuyangThemeSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WuyangThemeSubjectDao extends BaseMapper<WuyangThemeSubject> {

    @Delete("DELETE FROM wuyang_theme_subject WHERE data_date = #{date}")
    void physicalDeleteByDate(@Param("date") LocalDate date);

    @Select("SELECT DISTINCT subject_name, subject_detail FROM wuyang_theme_subject WHERE data_date = #{date} AND deleted = 0")
    List<WuyangThemeSubject> findSubjectsByDate(@Param("date") LocalDate date);

    @Select("SELECT subject_name, subject_detail, SUM(CASE WHEN stock_code <> '' AND is_zt = TRUE THEN 1 ELSE 0 END) AS ztCount " +
            "FROM wuyang_theme_subject WHERE data_date = #{date} AND deleted = 0 " +
            "GROUP BY subject_name, subject_detail " +
            "ORDER BY ztCount DESC, subject_name")
    List<WuyangThemeSubject> findSubjectsWithZtCountByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(DISTINCT subject_name) FROM wuyang_theme_subject WHERE data_date = #{date} AND deleted = 0")
    int countDistinctSubjectsByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM wuyang_theme_subject WHERE data_date = #{date} AND deleted = 0 AND stock_code <> ''")
    int countStocksByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM wuyang_theme_subject WHERE data_date = #{date} AND deleted = 0 AND stock_code = #{stockCode} AND stock_code <> '' ORDER BY subject_name")
    List<WuyangThemeSubject> findByDateAndStockCode(@Param("date") LocalDate date, @Param("stockCode") String stockCode);

    @Select("SELECT * FROM wuyang_theme_subject WHERE data_date = #{date} AND subject_name = #{subjectName} AND deleted = 0 AND stock_code <> ''")
    List<WuyangThemeSubject> findStocksByDateAndSubjectName(@Param("date") LocalDate date, @Param("subjectName") String subjectName);
}
