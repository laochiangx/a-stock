package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.StockBasic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票基本信息数据访问层
 */
@Mapper
public interface StockBasicDao extends BaseMapper<StockBasic> {
}