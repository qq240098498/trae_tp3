package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private BigDecimal maxDiscountAmount;
    private Integer validDays;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private Boolean pointsCompatible;
    private Integer totalCount;
    private Integer usedCount;
    private Integer issuedCount;
    private String status;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
