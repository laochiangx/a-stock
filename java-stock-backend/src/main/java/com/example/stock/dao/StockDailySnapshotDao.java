package com.example.stock.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stock.entity.StockDailySnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockDailySnapshotDao extends BaseMapper<StockDailySnapshot> {
}
