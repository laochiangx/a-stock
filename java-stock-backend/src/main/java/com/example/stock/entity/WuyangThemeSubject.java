package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wuyang_theme_subject")
public class WuyangThemeSubject {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("subject_name")
    private String subjectName;

    @TableField("subject_detail")
    private String subjectDetail;

    @TableField("stock_code")
    private String stockCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("lb_count")
    private String lbCount;

    @TableField("first_zt_time")
    private String firstZtTime;

    @TableField("last_zt_time")
    private String lastZtTime;

    @TableField("price")
    private BigDecimal price;

    @TableField("percent")
    private BigDecimal percent;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("reason")
    private String reason;

    @TableField("is_zt")
    private Boolean isZt;

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
