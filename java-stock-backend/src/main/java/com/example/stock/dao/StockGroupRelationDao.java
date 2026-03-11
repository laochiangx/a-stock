package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.StockGroupRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票分组关联数据访问层
 */
@Mapper
public interface StockGroupRelationDao extends BaseMapper<StockGroupRelation> {
}
