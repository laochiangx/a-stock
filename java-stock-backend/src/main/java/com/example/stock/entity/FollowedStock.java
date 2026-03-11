package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自选股实体类
 */
@Data
@TableName("followed_stock")
public class FollowedStock {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 股票代码 (如 sh600519, sz000001)
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 持仓数量
     */
    private Integer volume;

    /**
     * 涨跌幅报警值
     */
    private BigDecimal alarmChangePercent;

    /**
     * 价格报警值
     */
    private BigDecimal alarmPrice;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * AI定时任务表达式
     */
    private String cron;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
