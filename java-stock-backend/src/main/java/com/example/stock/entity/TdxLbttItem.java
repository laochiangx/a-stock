package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tdx_lbtt_item")
public class TdxLbttItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("max_level")
    private Integer maxLevel;

    @TableField("level")
    private Integer level;

    @TableField("stock_code")
    private String stockCode;

    @TableField("market")
    private String market;

    @TableField("stock_name")
    private String stockName;

    @TableField("reason")
    private String reason;

    @TableField("reason2")
    private String reason2;

    @TableField("zt_time")
    private String ztTime;

    @TableField("seal_amount")
    private BigDecimal sealAmount;

    @TableField("open_times")
    private Integer openTimes;

    @TableField("industry")
    private String industry;

    @TableField("zt_state")
    private Integer ztState;

    @TableField("promote_rate")
    private BigDecimal promoteRate;

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
