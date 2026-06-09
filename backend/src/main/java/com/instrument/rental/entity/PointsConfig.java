package com.instrument.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("points_config")
public class PointsConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configName;
    private BigDecimal configValue;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
