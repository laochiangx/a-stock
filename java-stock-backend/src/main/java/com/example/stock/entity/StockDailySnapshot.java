package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_daily_snapshot")
public class StockDailySnapshot {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private LocalDate dataDate;

    private String stockCode;

    private String stockName;

    private String sources;

    private Integer dcpm;

    private Integer rqbpm;

    private Integer thspm;

    private Integer shy;

    private BigDecimal zf;

    private BigDecimal zlje;

    private BigDecimal zljzb;

    private Boolean preselected;

    private Boolean dragonTiger;

    private String dragonTigerExplain;

    private String evidence;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
