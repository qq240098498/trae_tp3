package com.instrument.rental.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReturnDTO {
    private Long orderId;
    private String instrumentCondition;
    private BigDecimal deductAmount;
    private String refundMethod;
    private String remark;
}
