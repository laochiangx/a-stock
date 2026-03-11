package com.example.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.stock.dao.WordDictionaryDao;
import com.example.stock.entity.WordDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/word-dictionary")
@CrossOrigin
public class WordDictionaryController {

    @Autowired
    private WordDictionaryDao wordDictionaryDao;

    /**
     * 获取词典列表
     */
    @GetMapping("/list")
    public Map<String, Object> getList(
            @RequestParam(required = false) String wordType,
            @RequestParam(required = false) String word,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        LambdaQueryWrapper<WordDictionary> wrapper = new LambdaQueryWrapper<>();
        
        if (wordType != null && !wordType.isEmpty()) {
            wrapper.eq(WordDictionary::getWordType, wordType);
        }
        
        if (word != null && !word.isEmpty()) {
            wrapper.like(WordDictionary::getWord, word);
        }
        
        wrapper.orderByDesc(WordDictionary::getFrequencyTotal)
               .orderByDesc(WordDictionary::getBaseWeight);
        
        Page<WordDictionary> pageResult = wordDictionaryDao.selectPage(
            new Page<>(page, pageSize), 
            wrapper
        );
        
        // 统计各类型数量
        Map<String, Long> stats = new HashMap<>();
        stats.put("positive", wordDictionaryDao.selectCount(
            new LambdaQueryWrapper<WordDictionary>().eq(WordDictionary::getWordType, "positive")));
        stats.put("negative", wordDictionaryDao.selectCount(
            new LambdaQueryWrapper<WordDictionary>().eq(WordDictionary::getWordType, "negative")));
        stats.put("industry", wordDictionaryDao.selectCount(
            new LambdaQueryWrapper<WordDictionary>().eq(WordDictionary::getWordType, "industry")));
        stats.put("concept", wordDictionaryDao.selectCount(
            new LambdaQueryWrapper<WordDictionary>().eq(WordDictionary::getWordType, "concept")));
        stats.put("normal", wordDictionaryDao.selectCount(
            new LambdaQueryWrapper<WordDictionary>().eq(WordDictionary::getWordType, "normal")));
        stats.put("total", wordDictionaryDao.selectCount(null));
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("stats", stats);
        
        return result;
    }

    /**
     * 添加词汇
     */
    @PostMapping("/add")
    public Map<String, Object> addWord(@RequestBody WordDictionary word) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查是否已存在
            LambdaQueryWrapper<WordDictionary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WordDictionary::getWord, word.getWord());
            WordDictionary existing = wordDictionaryDao.selectOne(wrapper);
            
            if (existing != null) {
                // 更新已有词汇
                existing.setWordType(word.getWordType());
                existing.setBaseWeight(word.getBaseWeight());
                existing.setSentimentValue(word.getSentimentValue());
                existing.setIndustry(word.getIndustry());
                existing.setUpdatedAt(LocalDateTime.now());
                wordDictionaryDao.updateById(existing);
                result.put("success", true);
                result.put("message", "词汇已更新");
            } else {
                // 新增词汇
                word.setIsSystem(false);
                word.setFrequencyTotal(0);
                word.setCreatedAt(LocalDateTime.now());
                word.setUpdatedAt(LocalDateTime.now());
                wordDictionaryDao.insert(word);
                result.put("success", true);
                result.put("message", "添加成功");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 删除词汇
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteWord(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            WordDictionary word = wordDictionaryDao.selectById(id);
            if (word == null) {
                result.put("success", false);
                result.put("message", "词汇不存在");
                return result;
            }
            
            if (word.getIsSystem()) {
                result.put("success", false);
                result.put("message", "系统词汇不能删除");
                return result;
            }
            
            wordDictionaryDao.deleteById(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 批量导入词汇
     */
    @PostMapping("/batch-import")
    public Map<String, Object> batchImport(@RequestBody List<WordDictionary> words) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        for (WordDictionary word : words) {
            try {
                word.setIsSystem(false);
                word.setFrequencyTotal(0);
                word.setCreatedAt(LocalDateTime.now());
                word.setUpdatedAt(LocalDateTime.now());
                wordDictionaryDao.insert(word);
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }
        
        result.put("success", true);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("message", String.format("导入完成：成功 %d 条，失败 %d 条", successCount, failCount));
        
        return result;
    }
}
