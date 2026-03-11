package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.TelegraphTags;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电报标签关联数据访问层
 */
@Mapper
public interface TelegraphTagsDao extends BaseMapper<TelegraphTags> {
}