package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.IndexBasic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指数基本信息数据访问层
 */
@Mapper
public interface IndexBasicDao extends BaseMapper<IndexBasic> {
}