package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.PointsRecord;

import java.util.List;

public interface PointsRecordService extends IService<PointsRecord> {

    List<PointsRecord> getRecordsByCustomerId(Long customerId);

    PointsRecord addRecord(Long customerId, Long orderId, String type, Integer points, java.math.BigDecimal relatedAmount, String remark);
}
