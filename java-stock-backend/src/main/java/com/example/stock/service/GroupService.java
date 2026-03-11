package com.example.stock.service;

import java.util.List;
import java.util.Map;

public interface GroupService {
    /**
     * 获取分组列表
     *
     * @return 分组列表
     */
    List<Map<String, Object>> getGroupList();

    /**
     * 添加分组
     *
     * @param groupName 分组名称
     * @return 操作结果
     */
    String addGroup(String groupName);

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 操作结果
     */
    String removeGroup(Integer groupId);

    /**
     * 更新分组排序
     *
     * @param groupId 分组ID
     * @param sort 排序值
     * @return 是否成功
     */
    boolean updateGroupSort(Integer groupId, Integer sort);

    /**
     * 初始化分组排序
     *
     * @return 是否成功
     */
    boolean initializeGroupSort();
}