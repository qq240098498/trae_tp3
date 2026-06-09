package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.CouponRecord;

import java.math.BigDecimal;
import java.util.List;

public interface CouponRecordService extends IService<CouponRecord> {

    CouponRecord addRecord(Long couponId, Long customerId, Long templateId, Long orderId,
                           String type, BigDecimal discountAmount, String operator, String remark);

    List<CouponRecord> getRecordsByCouponId(Long couponId);

    List<CouponRecord> getRecordsByCustomerId(Long customerId);

    List<CouponRecord> getRecordsByOrderId(Long orderId);

    List<CouponRecord> getRecordsByTemplateId(Long templateId);
}
