package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.HotWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 热词数据访问层
 */
@Mapper
public interface HotWordDao extends BaseMapper<HotWord> {

    /**
     * 查询指定日期和来源的热词（按得分降序）
     */
    @Select("SELECT * FROM hot_word WHERE data_date = #{dataDate} AND source = #{source} ORDER BY score DESC LIMIT #{limit}")
    List<HotWord> findByDateAndSource(@Param("dataDate") LocalDate dataDate, 
                                       @Param("source") String source, 
                                       @Param("limit") int limit);

    /**
     * 查询最近24小时的热词（按得分降序）
     */
    @Select("SELECT word, SUM(frequency) as frequency, AVG(weight) as weight, SUM(score) as score, source " +
            "FROM hot_word WHERE data_date >= #{startDate} AND source = #{source} " +
            "GROUP BY word, source ORDER BY score DESC LIMIT #{limit}")
    List<HotWord> findLast24Hours(@Param("startDate") LocalDate startDate, 
                                   @Param("source") String source, 
                                   @Param("limit") int limit);
}
