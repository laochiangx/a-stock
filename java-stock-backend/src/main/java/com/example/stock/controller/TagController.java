package com.example.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.TagsDao;
import com.example.stock.entity.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin
public class TagController {

    @Autowired
    private TagsDao tagsDao;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String type,
                                    @RequestParam(required = false) String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<Tags> qw = new LambdaQueryWrapper<>();
            if (type != null && !type.isEmpty()) {
                qw.eq(Tags::getType, type);
            }
            if (name != null && !name.isEmpty()) {
                qw.like(Tags::getName, name);
            }
            qw.orderByDesc(Tags::getPriority).orderByAsc(Tags::getId);
            List<Tags> list = tagsDao.selectList(qw);
            result.put("success", true);
            result.put("list", list);
            result.put("total", list.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Tags tag) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (tag == null || tag.getName() == null || tag.getName().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "name is required");
                return result;
            }

            Tags existing = tagsDao.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tags>()
                    .eq("name", tag.getName().trim()));

            LocalDateTime now = LocalDateTime.now();
            if (existing != null) {
                existing.setType(tag.getType());
                existing.setPriority(tag.getPriority() == null ? 0 : tag.getPriority());
                existing.setUpdatedAt(now);
                tagsDao.updateById(existing);
                result.put("success", true);
                result.put("data", existing);
                result.put("message", "updated");
                return result;
            }

            tag.setName(tag.getName().trim());
            tag.setPriority(tag.getPriority() == null ? 0 : tag.getPriority());
            tag.setCreatedAt(now);
            tag.setUpdatedAt(now);
            tag.setDeleted(0);
            tagsDao.insert(tag);
            result.put("success", true);
            result.put("data", tag);
            result.put("message", "created");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Tags tag) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (tag == null || tag.getId() == null) {
                result.put("success", false);
                result.put("message", "id is required");
                return result;
            }
            Tags existing = tagsDao.selectById(tag.getId());
            if (existing == null) {
                result.put("success", false);
                result.put("message", "not found");
                return result;
            }

            if (tag.getName() != null && !tag.getName().trim().isEmpty()) {
                existing.setName(tag.getName().trim());
            }
            existing.setType(tag.getType());
            existing.setPriority(tag.getPriority() == null ? 0 : tag.getPriority());
            existing.setUpdatedAt(LocalDateTime.now());
            tagsDao.updateById(existing);

            result.put("success", true);
            result.put("data", existing);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            int n = tagsDao.deleteById(id);
            result.put("success", n > 0);
            result.put("deleted", n);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
