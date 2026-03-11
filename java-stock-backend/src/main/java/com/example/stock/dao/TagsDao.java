package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.Tags;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签数据访问层
 */
@Mapper
public interface TagsDao extends BaseMapper<Tags> {
}