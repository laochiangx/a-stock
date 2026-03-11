package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tdx_lbtt_day")
public class TdxLbttDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("rising_count")
    private Integer risingCount;

    @TableField("falling_count")
    private Integer fallingCount;

    @TableField("zt_all_count")
    private Integer ztAllCount;

    @TableField("zt_count")
    private Integer ztCount;

    @TableField("dt_count")
    private Integer dtCount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("last_total_amount")
    private BigDecimal lastTotalAmount;

    @TableField("hot1_name")
    private String hot1Name;

    @TableField("hot1_count")
    private Integer hot1Count;

    @TableField("hot2_name")
    private String hot2Name;

    @TableField("hot2_count")
    private Integer hot2Count;

    @TableField("hot3_name")
    private String hot3Name;

    @TableField("hot3_count")
    private Integer hot3Count;

    @TableField("raw_json")
    private String rawJson;

    @TableField("pbsdstat_json")
    private String pbsdstatJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
