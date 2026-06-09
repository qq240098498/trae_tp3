package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_record")
public class CouponRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long couponId;
    private Long customerId;
    private Long templateId;
    private Long orderId;
    private String type;
    private BigDecimal discountAmount;
    private String operator;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
