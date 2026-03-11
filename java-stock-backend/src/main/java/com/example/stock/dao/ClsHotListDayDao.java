package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.ClsHotListDay;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ClsHotListDayDao extends BaseMapper<ClsHotListDay> {

    @Delete("DELETE FROM cls_hot_list_day WHERE data_date = #{date} AND list_type = #{listType}")
    void physicalDelete(@Param("date") LocalDate date,
                        @Param("listType") String listType);

    @Select("SELECT * FROM cls_hot_list_day WHERE data_date = #{date} AND list_type = #{listType} AND deleted = 0 ORDER BY order_num ASC")
    List<ClsHotListDay> findByDate(@Param("date") LocalDate date,
                                   @Param("listType") String listType);

    @Select("SELECT COUNT(*) FROM cls_hot_list_day WHERE data_date = #{date} AND list_type = #{listType} AND deleted = 0")
    int countByDate(@Param("date") LocalDate date,
                    @Param("listType") String listType);
}
