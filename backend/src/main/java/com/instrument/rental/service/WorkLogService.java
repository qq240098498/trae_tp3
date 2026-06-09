package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.entity.WorkLog;

import java.math.BigDecimal;
import java.util.List;

public interface WorkLogService extends IService<WorkLog> {

    void logRentalCreate(Long orderId, String orderNo, Long customerId, String customerName,
                         Long instrumentId, String instrumentName, BigDecimal totalRent,
                         BigDecimal depositAmount, BigDecimal actualPayAmount,
                         Boolean useDiscount, String discountType,
                         BigDecimal couponDeductAmount, Long couponId,
                         BigDecimal pointsDeductAmount, Integer usedPoints,
                         String payMethod, String operator, String remark);

    void logRentalRenew(Long orderId, String orderNo, Long customerId, String customerName,
                        Long instrumentId, String instrumentName, BigDecimal additionalRent,
                        String operator, String remark);

    void logRentalReturn(Long orderId, String orderNo, Long customerId, String customerName,
                         Long instrumentId, String instrumentName, BigDecimal totalCharge,
                         BigDecimal overdueFee, BigDecimal deductAmount, BigDecimal refundAmount,
                         Integer earnedPoints, String operator, String remark);

    void logCouponIssue(Long couponId, String couponNo, Long templateId, String templateName,
                        Long customerId, String customerName, BigDecimal discountValue,
                        String couponType, String operator, String remark);

    void logCouponUse(Long couponId, String couponNo, Long templateId, String templateName,
                      Long customerId, String customerName, Long orderId, String orderNo,
                      BigDecimal orderAmount, BigDecimal discountAmount, String operator);

    void logCouponRevoke(Long couponId, String couponNo, Long templateId, String templateName,
                         Long customerId, String customerName, String operator, String remark);

    void logDepositCollect(Long orderId, String orderNo, Long customerId, String customerName,
                           BigDecimal amount, String payMethod, String operator, String remark);

    void logDepositRefund(Long orderId, String orderNo, Long customerId, String customerName,
                          BigDecimal amount, String payMethod, String operator, String remark);

    void logDepositDeduct(Long orderId, String orderNo, Long customerId, String customerName,
                          BigDecimal amount, String operator, String remark);

    void logPointsDeduct(Long customerId, String customerName, Long orderId, String orderNo,
                         Integer points, BigDecimal deductAmount, String operator, String remark);

    void logPointsEarn(Long customerId, String customerName, Long orderId, String orderNo,
                       Integer points, BigDecimal relatedAmount, String operator, String remark);

    Page<WorkLog> pageQuery(PageQuery pageQuery, String processType, Long customerId, Long orderId, String operator);

    List<WorkLog> getByOrderId(Long orderId);

    List<WorkLog> getByCustomerId(Long customerId);
}
