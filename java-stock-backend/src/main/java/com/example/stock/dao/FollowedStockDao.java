package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.FollowedStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 自选股数据访问层
 */
@Mapper
public interface FollowedStockDao extends BaseMapper<FollowedStock> {

    /**
     * 根据分组ID查询自选股
     */
    @Select("SELECT fs.* FROM followed_stock fs " +
            "INNER JOIN stock_group_relation sgr ON fs.stock_code = sgr.stock_code " +
            "WHERE sgr.group_id = #{groupId} " +
            "ORDER BY fs.sort ASC")
    List<FollowedStock> selectByGroupId(@Param("groupId") Integer groupId);
}
