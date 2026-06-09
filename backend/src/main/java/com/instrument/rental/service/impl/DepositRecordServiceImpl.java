package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.DepositRecord;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.mapper.DepositRecordMapper;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.DepositRecordService;
import com.instrument.rental.service.RentalOrderService;
import com.instrument.rental.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DepositRecordServiceImpl extends ServiceImpl<DepositRecordMapper, DepositRecord> implements DepositRecordService {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private DepositRecordMapper depositRecordMapper;

    @Autowired
    private CustomerService customerService;

    @Lazy
    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private WorkLogService workLogService;

    @Override
    @Transactional
    public DepositRecord collectDeposit(Long orderId, BigDecimal amount, String payMethod, String operator, String remark) {
        DepositRecord record = new DepositRecord();
        record.setOrderId(orderId);
        record.setType("COLLECT");
        record.setAmount(amount);
        record.setPayMethod(payMethod);
        record.setStatus("PAID");
        record.setRemark(remark);
        this.save(record);

        RentalOrder order = rentalOrderService.getById(orderId);
        if (order != null) {
            Customer customer = customerService.getById(order.getCustomerId());
            workLogService.logDepositCollect(
                    orderId,
                    order.getOrderNo(),
                    order.getCustomerId(),
                    customer != null ? customer.getName() : null,
                    amount,
                    payMethod,
                    operator,
                    remark
            );
        }

        return record;
    }

    @Override
    @Transactional
    public DepositRecord refundDeposit(Long orderId, BigDecimal amount, String payMethod, String operator, String remark) {
        RuntimeException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int rows = depositRecordMapper.insertRefundWithBalanceCheck(orderId, amount, payMethod, remark);
                if (rows > 0) {
                    DepositRecord record = new DepositRecord();
                    record.setOrderId(orderId);
                    record.setType("REFUND");
                    record.setAmount(amount);
                    record.setPayMethod(payMethod);
                    record.setStatus("PAID");
                    record.setRemark(remark);

                    RentalOrder order = rentalOrderService.getById(orderId);
                    if (order != null) {
                        Customer customer = customerService.getById(order.getCustomerId());
                        workLogService.logDepositRefund(
                                orderId,
                                order.getOrderNo(),
                                order.getCustomerId(),
                                customer != null ? customer.getName() : null,
                                amount,
                                payMethod,
                                operator,
                                remark
                        );
                    }

                    return record;
                }
                BigDecimal available = getAvailableDeposit(orderId);
                lastEx = new RuntimeException("退还金额超过可退余额，可退余额：" + available.toPlainString());
            } catch (Exception e) {
                lastEx = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
            }
            if (attempt < MAX_RETRIES) {
                sleep(50L * attempt);
            }
        }
        throw lastEx != null ? lastEx : new RuntimeException("押金退还失败，请稍后重试");
    }

    @Override
    @Transactional
    public DepositRecord deductDeposit(Long orderId, BigDecimal amount, String operator, String remark) {
        RuntimeException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int rows = depositRecordMapper.insertDeductWithBalanceCheck(orderId, amount, remark);
                if (rows > 0) {
                    DepositRecord record = new DepositRecord();
                    record.setOrderId(orderId);
                    record.setType("DEDUCT");
                    record.setAmount(amount);
                    record.setStatus("PAID");
                    record.setRemark(remark);

                    RentalOrder order = rentalOrderService.getById(orderId);
                    if (order != null) {
                        Customer customer = customerService.getById(order.getCustomerId());
                        workLogService.logDepositDeduct(
                                orderId,
                                order.getOrderNo(),
                                order.getCustomerId(),
                                customer != null ? customer.getName() : null,
                                amount,
                                operator,
                                remark
                        );
                    }

                    return record;
                }
                BigDecimal available = getAvailableDeposit(orderId);
                lastEx = new RuntimeException("扣除金额超过可扣余额，可扣余额：" + available.toPlainString());
            } catch (Exception e) {
                lastEx = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
            }
            if (attempt < MAX_RETRIES) {
                sleep(50L * attempt);
            }
        }
        throw lastEx != null ? lastEx : new RuntimeException("押金扣除失败，请稍后重试");
    }

    @Override
    public BigDecimal getAvailableDeposit(Long orderId) {
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepositRecord::getOrderId, orderId);
        wrapper.in(DepositRecord::getStatus, "PAID", "COMPLETED");
        List<DepositRecord> records = this.list(wrapper);

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal deducted = BigDecimal.ZERO;

        for (DepositRecord r : records) {
            switch (r.getType()) {
                case "COLLECT" -> collected = collected.add(r.getAmount());
                case "REFUND" -> refunded = refunded.add(r.getAmount());
                case "DEDUCT" -> deducted = deducted.add(r.getAmount());
            }
        }
        return collected.subtract(refunded).subtract(deducted);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
