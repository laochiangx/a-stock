package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dc_hot_list_day")
public class DcHotListDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("list_type")
    private String listType;

    @TableField("stock_code")
    private String stockCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("hot_score")
    private BigDecimal hotScore;

    @TableField("rise_and_fall")
    private BigDecimal riseAndFall;

    @TableField("order_num")
    private Integer orderNum;

    @TableField("raw_json")
    private String rawJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
