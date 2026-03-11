package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.stock.dao.StockGroupDao;
import com.example.stock.dao.StockGroupRelationDao;
import com.example.stock.entity.StockGroup;
import com.example.stock.entity.StockGroupRelation;
import com.example.stock.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private StockGroupDao stockGroupDao;

    @Autowired
    private StockGroupRelationDao stockGroupRelationDao;

    @Override
    public List<Map<String, Object>> getGroupList() {
        LambdaQueryWrapper<StockGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StockGroup::getSort);
        List<StockGroup> groups = stockGroupDao.selectList(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (StockGroup group : groups) {
            result.add(convertToMap(group));
        }
        return result;
    }

    @Override
    @Transactional
    public String addGroup(String groupName) {
        // 检查是否已存在同名分组
        LambdaQueryWrapper<StockGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockGroup::getName, groupName);
        StockGroup existing = stockGroupDao.selectOne(wrapper);
        
        if (existing != null) {
            return "分组已存在";
        }
        
        // 获取当前最大排序值
        LambdaQueryWrapper<StockGroup> maxSortWrapper = new LambdaQueryWrapper<>();
        maxSortWrapper.orderByDesc(StockGroup::getSort).last("LIMIT 1");
        StockGroup maxSortGroup = stockGroupDao.selectOne(maxSortWrapper);
        int nextSort = (maxSortGroup != null && maxSortGroup.getSort() != null) ? maxSortGroup.getSort() + 1 : 1;
        
        // 创建新分组
        StockGroup newGroup = new StockGroup();
        newGroup.setName(groupName);
        newGroup.setSort(nextSort);
        stockGroupDao.insert(newGroup);
        
        return "分组添加成功";
    }

    @Override
    @Transactional
    public String removeGroup(Integer groupId) {
        int deleted = stockGroupDao.deleteById(groupId);
        
        if (deleted > 0) {
            // 同时删除分组关联
            LambdaQueryWrapper<StockGroupRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(StockGroupRelation::getGroupId, groupId);
            stockGroupRelationDao.delete(relationWrapper);
            
            return "分组删除成功";
        }
        return "分组不存在";
    }

    @Override
    public boolean updateGroupSort(Integer groupId, Integer sort) {
        LambdaUpdateWrapper<StockGroup> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StockGroup::getId, groupId)
               .set(StockGroup::getSort, sort);
        
        int updated = stockGroupDao.update(null, wrapper);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean initializeGroupSort() {
        // 获取所有分组并按ID排序
        LambdaQueryWrapper<StockGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StockGroup::getId);
        List<StockGroup> groups = stockGroupDao.selectList(wrapper);
        
        // 重新设置排序值
        for (int i = 0; i < groups.size(); i++) {
            StockGroup group = groups.get(i);
            LambdaUpdateWrapper<StockGroup> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(StockGroup::getId, group.getId())
                         .set(StockGroup::getSort, i + 1);
            stockGroupDao.update(null, updateWrapper);
        }
        
        return true;
    }
    
    /**
     * 将实体转换为Map（兼容前端格式）
     */
    private Map<String, Object> convertToMap(StockGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", group.getId().intValue());
        map.put("name", group.getName());
        map.put("sort", group.getSort() != null ? group.getSort() : 999);
        return map;
    }
}
