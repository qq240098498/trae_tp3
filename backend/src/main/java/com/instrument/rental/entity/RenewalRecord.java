package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("renewal_record")
public class RenewalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private LocalDate originalEndDate;
    private LocalDate newEndDate;
    private BigDecimal additionalRent;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
