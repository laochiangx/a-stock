package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.Telegraph;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电报数据访问层
 */
@Mapper
public interface TelegraphDao extends BaseMapper<Telegraph> {
}