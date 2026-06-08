package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reminder")
public class Reminder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long customerId;
    private LocalDate expireDate;
    private Integer daysBeforeExpire;
    private String status;
    private String notifyMethod;
    private LocalDateTime notifyTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
