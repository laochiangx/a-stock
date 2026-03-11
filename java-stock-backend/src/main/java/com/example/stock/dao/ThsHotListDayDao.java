package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.ThsHotListDay;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ThsHotListDayDao extends BaseMapper<ThsHotListDay> {

    @Delete("DELETE FROM ths_hot_list_day WHERE data_date = #{date} AND stock_type = #{stockType} AND rank_type = #{rankType} AND list_type = #{listType}")
    void physicalDelete(@Param("date") LocalDate date,
                        @Param("stockType") String stockType,
                        @Param("rankType") String rankType,
                        @Param("listType") String listType);

    @Select("SELECT * FROM ths_hot_list_day WHERE data_date = #{date} AND stock_type = #{stockType} AND rank_type = #{rankType} AND list_type = #{listType} AND deleted = 0 ORDER BY order_num ASC")
    List<ThsHotListDay> findByDate(@Param("date") LocalDate date,
                                  @Param("stockType") String stockType,
                                  @Param("rankType") String rankType,
                                  @Param("listType") String listType);

    @Select("SELECT COUNT(*) FROM ths_hot_list_day WHERE data_date = #{date} AND stock_type = #{stockType} AND rank_type = #{rankType} AND list_type = #{listType} AND deleted = 0")
    int countByDate(@Param("date") LocalDate date,
                    @Param("stockType") String stockType,
                    @Param("rankType") String rankType,
                    @Param("listType") String listType);
}
