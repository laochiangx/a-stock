package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.WordDictionary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordDictionaryDao extends BaseMapper<WordDictionary> {
}
