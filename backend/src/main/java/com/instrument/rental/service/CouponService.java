package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService extends IService<Coupon> {

    Coupon issueCoupon(Long templateId, Long customerId, String operator, String remark);

    List<Coupon> issueCouponsBatch(Long templateId, List<Long> customerIds, String operator, String remark);

    Coupon getCouponById(Long id);

    Coupon getCouponByNo(String couponNo);

    List<Coupon> getCustomerCoupons(Long customerId);

    List<Coupon> getCustomerAvailableCoupons(Long customerId);

    List<Coupon> getCustomerAvailableCouponsForAmount(Long customerId, BigDecimal amount);

    Coupon useCoupon(Long couponId, Long orderId, BigDecimal orderAmount);

    boolean expireCoupon(Long couponId);

    boolean revokeCoupon(Long couponId, String operator, String remark);

    BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount);

    boolean checkCouponAvailable(Coupon coupon, BigDecimal orderAmount);

    void checkAndExpireCoupons();
}
