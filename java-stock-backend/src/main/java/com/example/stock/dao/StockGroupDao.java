package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.StockGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票分组数据访问层
 */
@Mapper
public interface StockGroupDao extends BaseMapper<StockGroup> {
}
