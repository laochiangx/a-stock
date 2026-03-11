package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 题材板块汇总实体
 */
@Data
@TableName("theme_plate_summary")
public class ThemePlateSummary {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDate dataDate;
    
    private String plateId;
    
    private String plateName;
    
    private String plateDescription;
    
    private Integer stockCount;
    
    private LocalDateTime createdAt;
}
