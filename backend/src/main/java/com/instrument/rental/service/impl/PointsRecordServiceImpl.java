package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.PointsRecord;
import com.instrument.rental.mapper.PointsRecordMapper;
import com.instrument.rental.service.PointsRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {

    @Override
    public List<PointsRecord> getRecordsByCustomerId(Long customerId) {
        return this.list(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getCustomerId, customerId)
                .orderByDesc(PointsRecord::getCreateTime));
    }

    @Override
    public PointsRecord addRecord(Long customerId, Long orderId, String type, Integer points, BigDecimal relatedAmount, String remark) {
        PointsRecord record = new PointsRecord();
        record.setCustomerId(customerId);
        record.setOrderId(orderId);
        record.setType(type);
        record.setPoints(points);
        record.setRelatedAmount(relatedAmount);
        record.setRemark(remark);
        this.save(record);
        return record;
    }
}
