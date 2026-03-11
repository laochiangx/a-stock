package com.example.stock.controller;

import com.example.stock.service.ThsHotListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ths-hot-list")
@CrossOrigin(origins = "*")
public class ThsHotListController {

    @Autowired
    private ThsHotListService thsHotListService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/day/ensure-table")
    public Map<String, Object> ensureDayTable() {
        Map<String, Object> result = new HashMap<>();
        result.put("currentUser", safeQueryString("select current_user"));
        result.put("currentSchema", safeQueryString("select current_schema"));
        result.put("searchPath", safeQueryString("show search_path"));
        result.put("existsBefore", tableExists("ths_hot_list_day"));

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ths_hot_list_day (" +
                            "id SERIAL PRIMARY KEY," +
                            "data_date DATE NOT NULL," +
                            "stock_type VARCHAR(20) NOT NULL," +
                            "rank_type VARCHAR(20) NOT NULL," +
                            "list_type VARCHAR(20) NOT NULL," +
                            "market INTEGER," +
                            "stock_code VARCHAR(20) NOT NULL," +
                            "stock_name VARCHAR(100)," +
                            "rate DECIMAL(20,4)," +
                            "rise_and_fall DECIMAL(20,8)," +
                            "analyse_title VARCHAR(500)," +
                            "analysis TEXT," +
                            "hot_rank_chg INTEGER," +
                            "topic_code VARCHAR(100)," +
                            "topic_title VARCHAR(500)," +
                            "topic_ios_jump_url VARCHAR(1000)," +
                            "topic_android_jump_url VARCHAR(1000)," +
                            "concept_tags TEXT," +
                            "popularity_tag VARCHAR(200)," +
                            "order_num INTEGER," +
                            "raw_json TEXT," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "deleted INTEGER DEFAULT 0," +
                            "UNIQUE(data_date, stock_type, rank_type, list_type, stock_code)" +
                            ")"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_date ON ths_hot_list_day (data_date)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_order ON ths_hot_list_day (data_date, order_num)");
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        result.put("existsAfter", tableExists("ths_hot_list_day"));
        return result;
    }

    private boolean tableExists(String tableName) {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?) IS NOT NULL",
                    Boolean.class,
                    tableName
            );
            return exists != null && exists;
        } catch (Exception e) {
            return false;
        }
    }

    private String safeQueryString(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return "<unknown>";
        }
    }

    @GetMapping("/day/sync")
    public Map<String, Object> syncDay(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Boolean force,
            @RequestParam(required = false) String stockType,
            @RequestParam(required = false) String listType) {
        return thsHotListService.syncDayHotList(date, force, stockType, listType);
    }

    @GetMapping("/day/list")
    public Map<String, Object> listDay(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String stockType,
            @RequestParam(required = false) String listType) {
        return thsHotListService.listDayHotList(date, stockType, listType);
    }
}
