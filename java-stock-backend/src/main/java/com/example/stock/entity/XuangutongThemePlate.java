package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("xuangutong_theme_plate")
public class XuangutongThemePlate {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("plate_id")
    private String plateId;

    @TableField("plate_name")
    private String plateName;

    @TableField("plate_description")
    private String plateDescription;

    @TableField("stock_code")
    private String stockCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("price")
    private Double price;

    @TableField("change_rate")
    private Double changeRate;

    @TableField("turnover_ratio")
    private Double turnoverRatio;

    @TableField("market_value")
    private Long marketValue;

    @TableField("is_up_limit")
    private Boolean isUpLimit;

    @TableField("enter_time")
    private Long enterTime;

    @TableField("m_days_n_boards")
    private String mDaysNBoards;

    @TableField("description")
    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
