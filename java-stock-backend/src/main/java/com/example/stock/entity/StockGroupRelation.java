package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 股票分组关联实体类
 */
@Data
@TableName("stock_group_relation")
public class StockGroupRelation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 分组ID
     */
    private Integer groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
