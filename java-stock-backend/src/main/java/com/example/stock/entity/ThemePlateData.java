package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 题材板块个股数据实体
 */
@Data
@TableName("theme_plate_data")
public class ThemePlateData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDate dataDate;
    
    private String plateName;
    
    private String plateDescription;
    
    private String stockCode;
    
    private String stockName;
    
    private BigDecimal price;
    
    private BigDecimal changeRate;
    
    private BigDecimal marketValue;
    
    private BigDecimal turnoverRatio;
    
    private Long enterTime;
    
    private Boolean isUpLimit;
    
    private String mDaysNBoards;
    
    private String description;
    
    private LocalDateTime createdAt;
}
