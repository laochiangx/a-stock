package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dongcai_theme_plate")
public class DongcaiThemePlate {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("plate_id")
    private Long plateId;

    @TableField("plate_name")
    private String plateName;

    @TableField("plate_description")
    private String plateDescription;

    @TableField("stock_code")
    private String stockCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("change_rate")
    private Double changeRate;

    @TableField("description")
    private String description;

    @TableField("lianban")
    private String lianban;

    @TableField(exist = false)
    private Integer ztCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
