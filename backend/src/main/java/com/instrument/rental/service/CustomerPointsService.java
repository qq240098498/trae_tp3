package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.CustomerPoints;

import java.math.BigDecimal;

public interface CustomerPointsService extends IService<CustomerPoints> {

    CustomerPoints getByCustomerId(Long customerId);

    CustomerPoints initCustomerPoints(Long customerId);

    int getAvailablePoints(Long customerId);

    boolean addPoints(Long customerId, Long orderId, Integer points, BigDecimal relatedAmount, String remark);

    boolean deductPoints(Long customerId, Long orderId, Integer points, BigDecimal relatedAmount, String remark);

    BigDecimal calculateDeductAmount(Integer points);

    Integer calculatePointsToDeduct(BigDecimal amount);

    Integer calculateEarnedPoints(BigDecimal amount);
}
