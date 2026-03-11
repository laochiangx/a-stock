package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.TdxLbttDay;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TdxLbttDayDao extends BaseMapper<TdxLbttDay> {

    @Delete("DELETE FROM tdx_lbtt_day WHERE data_date = #{date}")
    void physicalDeleteByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM tdx_lbtt_day WHERE data_date = #{date} AND deleted = 0")
    int countByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM tdx_lbtt_day WHERE data_date >= #{start} AND data_date <= #{end} AND deleted = 0 ORDER BY data_date ASC")
    List<TdxLbttDay> findByRange(@Param("start") LocalDate start,
                                 @Param("end") LocalDate end);
}
