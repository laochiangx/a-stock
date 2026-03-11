package com.example.stock.controller;

import com.example.stock.service.ThemePlateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 题材板块控制器
 * 获取选股通和东财的题材板块和个股数据
 */
@RestController
@RequestMapping("/api/theme-plate")
@CrossOrigin(origins = "*")
public class ThemePlateController {

    @Autowired
    private ThemePlateService themePlateService;

    /**
     * 同步选股通题材板块数据到数据库
     * 首次进入页面时调用，从API获取数据并保存到数据库
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @PostMapping("/sync")
    public Map<String, Object> syncData(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        return themePlateService.syncData(date, Boolean.TRUE.equals(force));
    }

    /**
     * 同步东财题材板块数据到数据库
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @PostMapping("/dongcai/sync")
    public Map<String, Object> syncDongcaiData(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        return themePlateService.syncDongcaiData(date, Boolean.TRUE.equals(force));
    }

    /**
     * 获取选股通题材板块数据
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @GetMapping("/plates")
    public Map<String, Object> getPlates(@RequestParam(required = false) String date) {
        return themePlateService.getPlates(date);
    }

    /**
     * 获取选股通板块个股数据
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @GetMapping("/stocks")
    public Map<String, Object> getStocks(@RequestParam(required = false) String date) {
        return themePlateService.getStocks(date);
    }

    /**
     * 获取完整的选股通题材板块数据（板块+个股，按板块分组）
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @GetMapping("/full")
    public Map<String, Object> getFullData(@RequestParam(required = false) String date) {
        return themePlateService.getFullData(date);
    }

    /**
     * 获取东财题材板块列表
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @GetMapping("/dongcai/plates")
    public Map<String, Object> getDongcaiPlates(@RequestParam(required = false) String date) {
        return themePlateService.getDongcaiPlates(date);
    }

    /**
     * 获取东财题材板块个股列表
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     * @param plateId 板块ID
     */
    @GetMapping("/dongcai/stocks")
    public Map<String, Object> getDongcaiStocks(
            @RequestParam(required = false) String date,
            @RequestParam(required = true) Long plateId) {
        return themePlateService.getDongcaiStocks(date, plateId);
    }

    /**
     * 获取完整的东财题材数据（板块+个股，按板块分组）
     * @param date 日期，格式：yyyy-MM-dd，不传则获取当天数据
     */
    @GetMapping("/dongcai/full")
    public Map<String, Object> getDongcaiFullData(@RequestParam(required = false) String date) {
        return themePlateService.getDongcaiFullData(date);
    }

    @GetMapping("/dongcai/hot-themes")
    public Map<String, Object> getDongcaiHotThemes(
            @RequestParam(required = false) Integer bdType) {
        return themePlateService.getDongcaiHotThemes(bdType);
    }

    @GetMapping("/dongcai/hot-theme/stocks")
    public Map<String, Object> getDongcaiHotThemeStocks(
            @RequestParam String themeCode,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer sort,
            @RequestParam(required = false) String sortField) {
        return themePlateService.getDongcaiHotThemeStocks(themeCode, pageNum, pageSize, sort, sortField);
    }

    @GetMapping("/dongcai/hot-boards")
    public Map<String, Object> getDongcaiHotBoards(
            @RequestParam Integer boardType,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize) {
        return themePlateService.getDongcaiHotBoards(boardType, pageNumber, pageSize);
    }

    @GetMapping("/dongcai/hot-board/stocks")
    public Map<String, Object> getDongcaiHotBoardStocks(
            @RequestParam String boardCode,
            @RequestParam(required = false) Integer pn,
            @RequestParam(required = false) Integer pz) {
        return themePlateService.getDongcaiHotBoardStocks(boardCode, pn, pz);
    }

    @PostMapping("/wuyang/sync")
    public Map<String, Object> syncWuyangData(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "false") Boolean force) {
        return themePlateService.syncWuyangData(date, Boolean.TRUE.equals(force));
    }

    @GetMapping("/wuyang/subjects")
    public Map<String, Object> getWuyangSubjects(@RequestParam(required = false) String date) {
        return themePlateService.getWuyangSubjects(date);
    }

    @GetMapping("/wuyang/stocks")
    public Map<String, Object> getWuyangStocks(
            @RequestParam(required = false) String date,
            @RequestParam(required = true) String subjectName) {
        return themePlateService.getWuyangStocks(date, subjectName);
    }
}
