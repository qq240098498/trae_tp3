package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.DepositRecord;

import java.math.BigDecimal;

public interface DepositRecordService extends IService<DepositRecord> {

    DepositRecord collectDeposit(Long orderId, BigDecimal amount, String payMethod, String remark);

    DepositRecord refundDeposit(Long orderId, BigDecimal amount, String payMethod, String remark);

    DepositRecord deductDeposit(Long orderId, BigDecimal amount, String remark);

    BigDecimal getAvailableDeposit(Long orderId);
}
