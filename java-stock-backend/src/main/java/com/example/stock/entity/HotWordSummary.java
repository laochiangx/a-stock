package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 热词统计汇总实体类
 */
@Data
@TableName("hot_word_summary")
public class HotWordSummary {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 词汇 */
    private String word;

    /** 汇总类型: daily/weekly/monthly */
    private String summaryType;

    /** 汇总日期 */
    private LocalDate summaryDate;

    /** 总频次 */
    private Integer totalFrequency;

    /** 平均权重 */
    private BigDecimal avgWeight;

    /** 总得分 */
    private BigDecimal totalScore;

    /** 出现在多少个来源 */
    private Integer sourceCount;

    /** 来源列表，逗号分隔 */
    private String sources;

    /** 词汇类型 */
    private String wordType;

    /** 趋势: up/down/stable */
    private String trend;

    /** 排名变化 */
    private Integer rankChange;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 汇总类型常量
    public static final String SUMMARY_DAILY = "daily";
    public static final String SUMMARY_WEEKLY = "weekly";
    public static final String SUMMARY_MONTHLY = "monthly";

    // 趋势常量
    public static final String TREND_UP = "up";
    public static final String TREND_DOWN = "down";
    public static final String TREND_STABLE = "stable";
}
