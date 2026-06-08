package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("instrument")
public class Instrument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String model;
    private String serialNo;
    private BigDecimal purchasePrice;
    private BigDecimal dailyRent;
    private BigDecimal depositAmount;
    private String status;
    @TableField(value = "`condition`")
    private String cond;
    private String imageUrl;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
