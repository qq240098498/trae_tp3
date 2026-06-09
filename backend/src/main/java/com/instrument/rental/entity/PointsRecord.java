package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long orderId;
    private String type;
    private Integer points;
    private BigDecimal relatedAmount;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
