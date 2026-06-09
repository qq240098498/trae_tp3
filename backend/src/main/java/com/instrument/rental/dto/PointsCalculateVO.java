package com.instrument.rental.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PointsCalculateVO {
    private Integer availablePoints;
    private BigDecimal availableDeductAmount;
    private Integer usePoints;
    private BigDecimal deductAmount;
    private BigDecimal totalRent;
    private BigDecimal actualPayAmount;
    private Integer willEarnPoints;
    private BigDecimal earnRate;
    private BigDecimal deductRate;
    private BigDecimal maxDeductPercent;
    private BigDecimal maxDeductAmount;
}
