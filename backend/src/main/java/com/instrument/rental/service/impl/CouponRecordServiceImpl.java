package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.CouponRecord;
import com.instrument.rental.mapper.CouponRecordMapper;
import com.instrument.rental.service.CouponRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CouponRecordServiceImpl extends ServiceImpl<CouponRecordMapper, CouponRecord> implements CouponRecordService {

    @Override
    public CouponRecord addRecord(Long couponId, Long customerId, Long templateId, Long orderId,
                                  String type, BigDecimal discountAmount, String operator, String remark) {
        CouponRecord record = new CouponRecord();
        record.setCouponId(couponId);
        record.setCustomerId(customerId);
        record.setTemplateId(templateId);
        record.setOrderId(orderId);
        record.setType(type);
        record.setDiscountAmount(discountAmount);
        record.setOperator(operator);
        record.setRemark(remark);
        this.save(record);
        return record;
    }

    @Override
    public List<CouponRecord> getRecordsByCouponId(Long couponId) {
        return this.list(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getCouponId, couponId)
                .orderByDesc(CouponRecord::getCreateTime));
    }

    @Override
    public List<CouponRecord> getRecordsByCustomerId(Long customerId) {
        return this.list(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getCustomerId, customerId)
                .orderByDesc(CouponRecord::getCreateTime));
    }

    @Override
    public List<CouponRecord> getRecordsByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getOrderId, orderId)
                .orderByDesc(CouponRecord::getCreateTime));
    }

    @Override
    public List<CouponRecord> getRecordsByTemplateId(Long templateId) {
        return this.list(new LambdaQueryWrapper<CouponRecord>()
                .eq(CouponRecord::getTemplateId, templateId)
                .orderByDesc(CouponRecord::getCreateTime));
    }
}
