package com.example.stock.controller;

import com.example.stock.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分组控制器
 */
@RestController
@RequestMapping("/api")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 获取分组列表
     *
     * @return 分组列表
     */
    @GetMapping("/groups")
    public List<Map<String, Object>> getGroupList() {
        return groupService.getGroupList();
    }

    /**
     * 添加分组
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @PostMapping("/groups")
    public String addGroup(@RequestBody Map<String, Object> request) {
        String groupName = (String) request.get("name");
        return groupService.addGroup(groupName);
    }

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 操作结果
     */
    @DeleteMapping("/groups/{groupId}")
    public String removeGroup(@PathVariable Integer groupId) {
        return groupService.removeGroup(groupId);
    }

    /**
     * 更新分组排序
     *
     * @param groupId 分组ID
     * @param request 请求参数
     * @return 是否成功
     */
    @PutMapping("/groups/{groupId}/sort")
    public boolean updateGroupSort(@PathVariable Integer groupId, @RequestBody Map<String, Object> request) {
        Integer sort = request.get("sort") != null ? Integer.valueOf(request.get("sort").toString()) : 0;
        return groupService.updateGroupSort(groupId, sort);
    }

    /**
     * 初始化分组排序
     *
     * @return 是否成功
     */
    @PostMapping("/groups/init-sort")
    public boolean initializeGroupSort() {
        return groupService.initializeGroupSort();
    }
}