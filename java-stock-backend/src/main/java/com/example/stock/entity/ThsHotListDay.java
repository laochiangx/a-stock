package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ths_hot_list_day")
public class ThsHotListDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    @TableField("stock_type")
    private String stockType;

    @TableField("rank_type")
    private String rankType;

    @TableField("list_type")
    private String listType;

    @TableField("market")
    private Integer market;

    @TableField("stock_code")
    private String stockCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("rate")
    private BigDecimal rate;

    @TableField("rise_and_fall")
    private BigDecimal riseAndFall;

    @TableField("analyse_title")
    private String analyseTitle;

    @TableField("analysis")
    private String analyse;

    @TableField("hot_rank_chg")
    private Integer hotRankChg;

    @TableField("topic_code")
    private String topicCode;

    @TableField("topic_title")
    private String topicTitle;

    @TableField("topic_ios_jump_url")
    private String topicIosJumpUrl;

    @TableField("topic_android_jump_url")
    private String topicAndroidJumpUrl;

    @TableField("concept_tags")
    private String conceptTags;

    @TableField("popularity_tag")
    private String popularityTag;

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
