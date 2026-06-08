package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.DepositRecord;
import com.instrument.rental.mapper.DepositRecordMapper;
import com.instrument.rental.service.DepositRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DepositRecordServiceImpl extends ServiceImpl<DepositRecordMapper, DepositRecord> implements DepositRecordService {

    private final ConcurrentHashMap<Long, ReentrantLock> orderLocks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(Long orderId) {
        return orderLocks.computeIfAbsent(orderId, k -> new ReentrantLock());
    }

    @Override
    @Transactional
    public DepositRecord collectDeposit(Long orderId, BigDecimal amount, String payMethod, String remark) {
        ReentrantLock lock = getLock(orderId);
        lock.lock();
        try {
            DepositRecord record = new DepositRecord();
            record.setOrderId(orderId);
            record.setType("COLLECT");
            record.setAmount(amount);
            record.setPayMethod(payMethod);
            record.setStatus("PAID");
            record.setRemark(remark);
            this.save(record);
            return record;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public DepositRecord refundDeposit(Long orderId, BigDecimal amount, String payMethod, String remark) {
        ReentrantLock lock = getLock(orderId);
        lock.lock();
        try {
            BigDecimal available = getAvailableDeposit(orderId);
            if (available.compareTo(amount) < 0) {
                throw new RuntimeException("退还金额超过可退余额，可退余额：" + available.toPlainString());
            }
            DepositRecord record = new DepositRecord();
            record.setOrderId(orderId);
            record.setType("REFUND");
            record.setAmount(amount);
            record.setPayMethod(payMethod);
            record.setStatus("PAID");
            record.setRemark(remark);
            this.save(record);
            return record;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public DepositRecord deductDeposit(Long orderId, BigDecimal amount, String remark) {
        ReentrantLock lock = getLock(orderId);
        lock.lock();
        try {
            BigDecimal available = getAvailableDeposit(orderId);
            if (available.compareTo(amount) < 0) {
                throw new RuntimeException("扣除金额超过可扣余额，可扣余额：" + available.toPlainString());
            }
            DepositRecord record = new DepositRecord();
            record.setOrderId(orderId);
            record.setType("DEDUCT");
            record.setAmount(amount);
            record.setStatus("PAID");
            record.setRemark(remark);
            this.save(record);
            return record;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigDecimal getAvailableDeposit(Long orderId) {
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepositRecord::getOrderId, orderId);
        wrapper.in(DepositRecord::getStatus, "PAID", "COMPLETED");
        java.util.List<DepositRecord> records = this.list(wrapper);

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
}
