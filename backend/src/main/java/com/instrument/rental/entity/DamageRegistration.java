package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("damage_registration")
public class DamageRegistration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long instrumentId;
    private Long customerId;
    private String damageType;
    private String description;
    private String severity;
    private BigDecimal estimatedCost;
    private String status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
