package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponNo;
    private Long templateId;
    private Long customerId;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private BigDecimal maxDiscountAmount;
    private Boolean pointsCompatible;
    private String status;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private Long orderId;
    private LocalDateTime usedTime;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
