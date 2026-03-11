package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("menu_config")
public class MenuConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String menuKey;
    private String menuName;
    private String menuIcon;
    private String menuPath;
    private String parentKey;
    private Integer sortOrder;
    private Boolean isVisible;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
