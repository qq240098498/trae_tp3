package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.entity.WorkLog;
import com.instrument.rental.mapper.WorkLogMapper;
import com.instrument.rental.service.WorkLogService;
import cn.hutool.core.util.StrUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkLogServiceImpl extends ServiceImpl<WorkLogMapper, WorkLog> implements WorkLogService {

    private WorkLog buildBase(String processType, String operator, String remark) {
        WorkLog log = new WorkLog();
        log.setProcessType(processType);
        log.setOperator(operator);
        log.setRemark(remark);
        log.setUseDiscount(false);
        return log;
    }

    @Override
    @Async
    public void logRentalCreate(Long orderId, String orderNo, Long customerId, String customerName,
                                Long instrumentId, String instrumentName, BigDecimal totalRent,
                                BigDecimal depositAmount, BigDecimal actualPayAmount,
                                Boolean useDiscount, String discountType,
                                BigDecimal couponDeductAmount, Long couponId,
                                BigDecimal pointsDeductAmount, Integer usedPoints,
                                String payMethod, String operator, String remark) {
        WorkLog log = buildBase("RENTAL_CREATE", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setInstrumentId(instrumentId);
        log.setInstrumentName(instrumentName);
        log.setAmount(actualPayAmount != null ? actualPayAmount : BigDecimal.ZERO);
        log.setUseDiscount(useDiscount);
        log.setDiscountType(discountType);
        log.setCouponDeductAmount(couponDeductAmount);
        log.setCouponId(couponId);
        log.setPointsDeductAmount(pointsDeductAmount);
        log.setUsedPoints(usedPoints);
        log.setPayMethod(payMethod);
        this.save(log);
    }

    @Override
    @Async
    public void logRentalRenew(Long orderId, String orderNo, Long customerId, String customerName,
                               Long instrumentId, String instrumentName, BigDecimal additionalRent,
                               String operator, String remark) {
        WorkLog log = buildBase("RENTAL_RENEW", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setInstrumentId(instrumentId);
        log.setInstrumentName(instrumentName);
        log.setAmount(additionalRent != null ? additionalRent : BigDecimal.ZERO);
        this.save(log);
    }

    @Override
    @Async
    public void logRentalReturn(Long orderId, String orderNo, Long customerId, String customerName,
                                Long instrumentId, String instrumentName, BigDecimal totalCharge,
                                BigDecimal overdueFee, BigDecimal deductAmount, BigDecimal refundAmount,
                                Integer earnedPoints, String operator, String remark) {
        WorkLog log = buildBase("RENTAL_RETURN", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setInstrumentId(instrumentId);
        log.setInstrumentName(instrumentName);
        log.setAmount(totalCharge != null ? totalCharge : BigDecimal.ZERO);
        log.setUsedPoints(earnedPoints);
        this.save(log);
    }

    @Override
    @Async
    public void logCouponIssue(Long couponId, String couponNo, Long templateId, String templateName,
                               Long customerId, String customerName, BigDecimal discountValue,
                               String couponType, String operator, String remark) {
        WorkLog log = buildBase("COUPON_ISSUE", operator, remark);
        log.setCouponId(couponId);
        log.setCouponNo(couponNo);
        log.setTemplateId(templateId);
        log.setTemplateName(templateName);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setAmount(discountValue);
        log.setDiscountType(couponType);
        this.save(log);
    }

    @Override
    @Async
    public void logCouponUse(Long couponId, String couponNo, Long templateId, String templateName,
                             Long customerId, String customerName, Long orderId, String orderNo,
                             BigDecimal orderAmount, BigDecimal discountAmount, String operator) {
        WorkLog log = buildBase("COUPON_USE", operator, "订单使用优惠券");
        log.setCouponId(couponId);
        log.setCouponNo(couponNo);
        log.setTemplateId(templateId);
        log.setTemplateName(templateName);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setAmount(orderAmount);
        log.setUseDiscount(true);
        log.setDiscountType("COUPON");
        log.setCouponDeductAmount(discountAmount);
        this.save(log);
    }

    @Override
    @Async
    public void logCouponRevoke(Long couponId, String couponNo, Long templateId, String templateName,
                                Long customerId, String customerName, String operator, String remark) {
        WorkLog log = buildBase("COUPON_REVOKE", operator, remark);
        log.setCouponId(couponId);
        log.setCouponNo(couponNo);
        log.setTemplateId(templateId);
        log.setTemplateName(templateName);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        this.save(log);
    }

    @Override
    @Async
    public void logDepositCollect(Long orderId, String orderNo, Long customerId, String customerName,
                                  BigDecimal amount, String payMethod, String operator, String remark) {
        WorkLog log = buildBase("DEPOSIT_COLLECT", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setAmount(amount);
        log.setPayMethod(payMethod);
        this.save(log);
    }

    @Override
    @Async
    public void logDepositRefund(Long orderId, String orderNo, Long customerId, String customerName,
                                 BigDecimal amount, String payMethod, String operator, String remark) {
        WorkLog log = buildBase("DEPOSIT_REFUND", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setAmount(amount);
        log.setPayMethod(payMethod);
        this.save(log);
    }

    @Override
    @Async
    public void logDepositDeduct(Long orderId, String orderNo, Long customerId, String customerName,
                                 BigDecimal amount, String operator, String remark) {
        WorkLog log = buildBase("DEPOSIT_DEDUCT", operator, remark);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setAmount(amount);
        this.save(log);
    }

    @Override
    @Async
    public void logPointsDeduct(Long customerId, String customerName, Long orderId, String orderNo,
                                Integer points, BigDecimal deductAmount, String operator, String remark) {
        WorkLog log = buildBase("POINTS_DEDUCT", operator, remark);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setAmount(deductAmount);
        log.setUseDiscount(true);
        log.setDiscountType("POINTS");
        log.setPointsDeductAmount(deductAmount);
        log.setUsedPoints(points);
        this.save(log);
    }

    @Override
    @Async
    public void logPointsEarn(Long customerId, String customerName, Long orderId, String orderNo,
                              Integer points, BigDecimal relatedAmount, String operator, String remark) {
        WorkLog log = buildBase("POINTS_EARN", operator, remark);
        log.setCustomerId(customerId);
        log.setCustomerName(customerName);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setAmount(relatedAmount);
        log.setUsedPoints(points);
        this.save(log);
    }

    @Override
    public Page<WorkLog> pageQuery(PageQuery pageQuery, String processType, Long customerId, Long orderId, String operator) {
        LambdaQueryWrapper<WorkLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageQuery.getKeyword())) {
            wrapper.and(w -> w.like(WorkLog::getOrderNo, pageQuery.getKeyword())
                    .or().like(WorkLog::getCustomerName, pageQuery.getKeyword())
                    .or().like(WorkLog::getCouponNo, pageQuery.getKeyword())
                    .or().like(WorkLog::getRemark, pageQuery.getKeyword()));
        }
        if (StrUtil.isNotBlank(processType)) {
            wrapper.eq(WorkLog::getProcessType, processType);
        }
        if (customerId != null) {
            wrapper.eq(WorkLog::getCustomerId, customerId);
        }
        if (orderId != null) {
            wrapper.eq(WorkLog::getOrderId, orderId);
        }
        if (StrUtil.isNotBlank(operator)) {
            wrapper.eq(WorkLog::getOperator, operator);
        }
        wrapper.orderByDesc(WorkLog::getCreateTime);
        return this.page(pageQuery.toPage(), wrapper);
    }

    @Override
    public List<WorkLog> getByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<WorkLog>()
                .eq(WorkLog::getOrderId, orderId)
                .orderByDesc(WorkLog::getCreateTime));
    }

    @Override
    public List<WorkLog> getByCustomerId(Long customerId) {
        return this.list(new LambdaQueryWrapper<WorkLog>()
                .eq(WorkLog::getCustomerId, customerId)
                .orderByDesc(WorkLog::getCreateTime));
    }
}
