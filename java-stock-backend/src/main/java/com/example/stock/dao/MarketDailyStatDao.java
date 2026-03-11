package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.MarketDailyStat;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MarketDailyStatDao extends BaseMapper<MarketDailyStat> {

    @Delete("DELETE FROM market_daily_stat WHERE data_date = #{date}")
    void physicalDeleteByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM market_daily_stat WHERE data_date = #{date} AND deleted = 0")
    MarketDailyStat findByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM market_daily_stat WHERE data_date = #{date} AND deleted = 0")
    int countByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM market_daily_stat WHERE deleted = 0 ORDER BY data_date DESC LIMIT #{limit}")
    List<MarketDailyStat> listRecent(@Param("limit") int limit);
}
