package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 24小时热词实体类（增强版）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hot_word")
public class HotWord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 热词 */
    private String word;

    /** 出现频次 */
    private Integer frequency;

    /** 权重 */
    private BigDecimal weight;

    /** 得分 = 频次 * 权重 */
    private BigDecimal score;

    /** 来源：全部、财联社电报、新浪财经、股通快速、同花顺快讯 */
    private String source;

    /** 数据日期 */
    private LocalDate dataDate;

    /** 数据小时（0-23） */
    private Integer dataHour;

    // ========== 新增字段：更细维度统计 ==========

    /** 词汇类型: positive/negative/industry/concept/stock/normal */
    private String wordType;

    /** 情感得分 */
    private BigDecimal sentimentScore;

    /** 所属行业 */
    private String industry;

    /** 关联股票代码，逗号分隔 */
    private String relatedStocks;

    /** 首次出现时间 */
    private LocalDateTime firstAppearTime;

    /** 最后出现时间 */
    private LocalDateTime lastAppearTime;

    /** 出现次数（跨小时累计） */
    private Integer appearCount;

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
