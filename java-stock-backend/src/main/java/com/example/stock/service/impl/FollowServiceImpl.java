package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.stock.dao.FollowedStockDao;
import com.example.stock.dao.StockGroupRelationDao;
import com.example.stock.entity.FollowedStock;
import com.example.stock.entity.StockGroupRelation;
import com.example.stock.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowedStockDao followedStockDao;

    @Autowired
    private StockGroupRelationDao stockGroupRelationDao;

    @Override
    public List<Map<String, Object>> getFollowList(Integer groupId) {
        List<FollowedStock> stocks;
        
        if (groupId == null || groupId == 0) {
            // 返回所有关注的股票
            LambdaQueryWrapper<FollowedStock> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(FollowedStock::getSort);
            stocks = followedStockDao.selectList(wrapper);
        } else {
            // 返回指定分组的股票
            stocks = followedStockDao.selectByGroupId(groupId);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (FollowedStock stock : stocks) {
            result.add(convertToMap(stock));
        }
        return result;
    }

    @Override
    @Transactional
    public String follow(String stockCode) {
        // 检查是否已关注
        LambdaQueryWrapper<FollowedStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode);
        FollowedStock existing = followedStockDao.selectOne(wrapper);
        
        if (existing != null) {
            return "已经关注了";
        }
        
        // 获取当前最大排序值
        LambdaQueryWrapper<FollowedStock> maxSortWrapper = new LambdaQueryWrapper<>();
        maxSortWrapper.orderByDesc(FollowedStock::getSort).last("LIMIT 1");
        FollowedStock maxSortStock = followedStockDao.selectOne(maxSortWrapper);
        int nextSort = (maxSortStock != null && maxSortStock.getSort() != null) ? maxSortStock.getSort() + 1 : 1;
        
        // 创建关注记录
        FollowedStock stock = new FollowedStock();
        stock.setStockCode(stockCode);
        stock.setStockName("");
        stock.setSort(nextSort);
        stock.setAlarmChangePercent(BigDecimal.ZERO);
        stock.setAlarmPrice(BigDecimal.ZERO);
        stock.setCostPrice(BigDecimal.ZERO);
        stock.setVolume(0);
        stock.setCron("");
        
        followedStockDao.insert(stock);
        
        return "关注成功";
    }

    @Override
    @Transactional
    public String unfollow(String stockCode) {
        LambdaQueryWrapper<FollowedStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode);
        
        int deleted = followedStockDao.delete(wrapper);
        
        if (deleted > 0) {
            // 同时删除分组关联
            LambdaQueryWrapper<StockGroupRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(StockGroupRelation::getStockCode, stockCode);
            stockGroupRelationDao.delete(relationWrapper);
            
            return "取消关注成功";
        }
        return "未关注该股票";
    }

    @Override
    public String setStockSort(String stockCode, Integer sort) {
        LambdaUpdateWrapper<FollowedStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode)
               .set(FollowedStock::getSort, sort);
        
        int updated = followedStockDao.update(null, wrapper);
        return updated > 0 ? "排序设置成功" : "股票未关注";
    }

    @Override
    public String setAlarmChangePercent(String stockCode, Double percent, Double alarmPrice) {
        LambdaUpdateWrapper<FollowedStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode)
               .set(FollowedStock::getAlarmChangePercent, percent != null ? BigDecimal.valueOf(percent) : BigDecimal.ZERO)
               .set(FollowedStock::getAlarmPrice, alarmPrice != null ? BigDecimal.valueOf(alarmPrice) : BigDecimal.ZERO);
        
        int updated = followedStockDao.update(null, wrapper);
        return updated > 0 ? "价格提醒设置成功" : "股票未关注";
    }

    @Override
    public String setCostPriceAndVolume(String stockCode, Double costPrice, Integer volume) {
        LambdaUpdateWrapper<FollowedStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode)
               .set(FollowedStock::getCostPrice, costPrice != null ? BigDecimal.valueOf(costPrice) : BigDecimal.ZERO)
               .set(FollowedStock::getVolume, volume != null ? volume : 0);
        
        int updated = followedStockDao.update(null, wrapper);
        return updated > 0 ? "成本价和数量设置成功" : "股票未关注";
    }

    @Override
    public String setStockAICron(String stockCode, String cron) {
        LambdaUpdateWrapper<FollowedStock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FollowedStock::getStockCode, stockCode)
               .set(FollowedStock::getCron, cron != null ? cron : "");
        
        int updated = followedStockDao.update(null, wrapper);
        return updated > 0 ? "AI定时任务设置成功" : "股票未关注";
    }

    @Override
    @Transactional
    public String addStockGroup(String stockCode, Integer groupId) {
        // 检查股票是否已关注
        LambdaQueryWrapper<FollowedStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(FollowedStock::getStockCode, stockCode);
        FollowedStock stock = followedStockDao.selectOne(stockWrapper);
        
        if (stock == null) {
            return "股票未关注";
        }
        
        // 检查是否已在该分组
        LambdaQueryWrapper<StockGroupRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(StockGroupRelation::getStockCode, stockCode)
                       .eq(StockGroupRelation::getGroupId, groupId);
        StockGroupRelation existing = stockGroupRelationDao.selectOne(relationWrapper);
        
        if (existing != null) {
            return "股票已在该分组中";
        }
        
        // 添加分组关联
        StockGroupRelation relation = new StockGroupRelation();
        relation.setStockCode(stockCode);
        relation.setGroupId(groupId);
        stockGroupRelationDao.insert(relation);
        
        return "添加分组成功";
    }

    @Override
    @Transactional
    public String removeStockGroup(String stockCode, Integer groupId) {
        LambdaQueryWrapper<StockGroupRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockGroupRelation::getStockCode, stockCode)
               .eq(StockGroupRelation::getGroupId, groupId);
        
        int deleted = stockGroupRelationDao.delete(wrapper);
        return deleted > 0 ? "移除分组成功" : "股票不在该分组中";
    }
    
    /**
     * 将实体转换为Map（兼容前端格式）
     */
    private Map<String, Object> convertToMap(FollowedStock stock) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", stock.getId());
        map.put("StockCode", stock.getStockCode());
        map.put("StockName", stock.getStockName() != null ? stock.getStockName() : "");
        map.put("CostPrice", stock.getCostPrice() != null ? stock.getCostPrice().doubleValue() : 0.0);
        map.put("Volume", stock.getVolume() != null ? stock.getVolume() : 0);
        map.put("AlarmChangePercent", stock.getAlarmChangePercent() != null ? stock.getAlarmChangePercent().doubleValue() : 0.0);
        map.put("AlarmPrice", stock.getAlarmPrice() != null ? stock.getAlarmPrice().doubleValue() : 0.0);
        map.put("Sort", stock.getSort() != null ? stock.getSort() : 999);
        map.put("Cron", stock.getCron() != null ? stock.getCron() : "");
        return map;
    }
}
