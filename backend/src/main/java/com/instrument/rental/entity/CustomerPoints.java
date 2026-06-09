package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer_points")
public class CustomerPoints {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Integer totalPoints;
    private Integer availablePoints;
    private Integer usedPoints;
    private Integer expiredPoints;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
