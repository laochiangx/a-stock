package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 动态词典实体类 - 自动学习新词汇
 */
@Data
@TableName("word_dictionary")
public class WordDictionary {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 词汇 */
    private String word;

    /** 词汇类型: positive/negative/industry/concept/stock/normal */
    private String wordType;

    /** 基础权重 */
    private BigDecimal baseWeight;

    /** 情感值：正数为正面，负数为负面 */
    private BigDecimal sentimentValue;

    /** 所属行业 */
    private String industry;

    /** 是否系统预置词 */
    private Boolean isSystem;

    /** 历史总出现次数 */
    private Integer frequencyTotal;

    /** 最后出现日期 */
    private LocalDate lastSeenDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 词汇类型常量
    public static final String TYPE_POSITIVE = "positive";
    public static final String TYPE_NEGATIVE = "negative";
    public static final String TYPE_INDUSTRY = "industry";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_STOCK = "stock";
    public static final String TYPE_NORMAL = "normal";
}
