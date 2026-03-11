package com.example.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("market_daily_stat")
public class MarketDailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("data_date")
    private LocalDate dataDate;

    private Integer risingCount;

    private Integer fallingCount;

    private Integer totalZt;

    private Integer totalDt;

    private Integer lbCount;

    @TableField("up_10_percent")
    private Integer up10Percent;

    @TableField("down_9_percent")
    private Integer down9Percent;

    private Integer yizi;

    private Integer firstBoardCount;

    private BigDecimal firstBoardRate;

    private Integer secondBoardCount;

    private BigDecimal secondBoardRate;

    private Integer thirdBoardCount;

    private BigDecimal thirdBoardRate;

    private Integer fourthBoardCount;

    private BigDecimal fourthBoardRate;

    private Integer fifthBoardAboveCount;

    private BigDecimal fifthBoardAboveRate;

    @TableField("zt_925")
    private Integer zt925;

    @TableField("before_10_count")
    private Integer before10Count;

    @TableField("before_10_rate")
    private BigDecimal before10Rate;

    @TableField("between_10_1130_count")
    private Integer between101130Count;

    @TableField("between_10_1130_rate")
    private BigDecimal between101130Rate;

    @TableField("between_13_14_count")
    private Integer between1314Count;

    @TableField("between_13_14_rate")
    private BigDecimal between1314Rate;

    @TableField("between_14_15_count")
    private Integer between1415Count;

    @TableField("between_14_15_rate")
    private BigDecimal between1415Rate;

    private Integer openCount;

    private BigDecimal fbRatio;

    private BigDecimal oneToTwoRatio;

    private BigDecimal twoToThreeRatio;

    private BigDecimal threeToFourRatio;

    private BigDecimal lbRatio;

    private BigDecimal yesterdayLbRatio;

    private BigDecimal ztAmount;

    private BigDecimal totalAmount;

    private BigDecimal shAmount;

    private BigDecimal chuangyeAmount;

    private BigDecimal kcAmount;

    private Integer marketScore;

    private String scoreGrade;

    private String scoreComponents;

    private String stageCode;

    private String stageName;

    private String stageReason;

    private String rawJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
