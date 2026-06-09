package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("work_log")
public class WorkLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String processType;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long instrumentId;

    private String instrumentName;

    private Long couponId;

    private String couponNo;

    private Long templateId;

    private String templateName;

    private BigDecimal amount;

    private Boolean useDiscount;

    private String discountType;

    private BigDecimal couponDeductAmount;

    private BigDecimal pointsDeductAmount;

    private Integer usedPoints;

    private String payMethod;

    private String operator;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
