package com.instrument.rental.dto;

import com.instrument.rental.entity.Coupon;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CouponCalculateVO {
    private BigDecimal totalRent;
    private Coupon selectedCoupon;
    private BigDecimal couponDeductAmount;
    private Boolean pointsCompatible;
    private Integer availablePoints;
    private BigDecimal availableDeductAmount;
    private Integer usePoints;
    private BigDecimal pointsDeductAmount;
    private BigDecimal actualPayAmount;
    private Integer willEarnPoints;
    private BigDecimal earnRate;
    private BigDecimal deductRate;
    private BigDecimal maxDeductPercent;
    private BigDecimal maxDeductAmount;
    private List<Coupon> availableCoupons;
    private String message;
}
