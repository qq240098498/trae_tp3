package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.CustomerPoints;
import com.instrument.rental.entity.PointsRecord;
import com.instrument.rental.mapper.CustomerPointsMapper;
import com.instrument.rental.service.CustomerPointsService;
import com.instrument.rental.service.PointsConfigService;
import com.instrument.rental.service.PointsRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CustomerPointsServiceImpl extends ServiceImpl<CustomerPointsMapper, CustomerPoints> implements CustomerPointsService {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private CustomerPointsMapper customerPointsMapper;

    @Autowired
    private PointsRecordService pointsRecordService;

    @Autowired
    private PointsConfigService pointsConfigService;

    @Override
    public CustomerPoints getByCustomerId(Long customerId) {
        CustomerPoints points = this.getOne(new LambdaQueryWrapper<CustomerPoints>().eq(CustomerPoints::getCustomerId, customerId));
        if (points == null) {
            points = initCustomerPoints(customerId);
        }
        return points;
    }

    @Override
    public CustomerPoints initCustomerPoints(Long customerId) {
        CustomerPoints points = new CustomerPoints();
        points.setCustomerId(customerId);
        points.setTotalPoints(0);
        points.setAvailablePoints(0);
        points.setUsedPoints(0);
        points.setExpiredPoints(0);
        this.save(points);
        return points;
    }

    @Override
    public int getAvailablePoints(Long customerId) {
        CustomerPoints points = getByCustomerId(customerId);
        return points.getAvailablePoints();
    }

    @Override
    @Transactional
    public boolean addPoints(Long customerId, Long orderId, Integer points, BigDecimal relatedAmount, String remark) {
        RuntimeException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int rows = customerPointsMapper.updatePointsWithBalanceCheck(customerId, points, points, 0);
                if (rows > 0) {
                    pointsRecordService.addRecord(customerId, orderId, "EARN", points, relatedAmount, remark);
                    return true;
                }
                lastEx = new RuntimeException("积分添加失败");
            } catch (Exception e) {
                lastEx = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
            }
            if (attempt < MAX_RETRIES) {
                sleep(50L * attempt);
            }
        }
        throw lastEx != null ? lastEx : new RuntimeException("积分添加失败，请稍后重试");
    }

    @Override
    @Transactional
    public boolean deductPoints(Long customerId, Long orderId, Integer points, BigDecimal relatedAmount, String remark) {
        RuntimeException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int rows = customerPointsMapper.updatePointsWithBalanceCheck(customerId, -points, 0, points);
                if (rows > 0) {
                    pointsRecordService.addRecord(customerId, orderId, "DEDUCT", -points, relatedAmount, remark);
                    return true;
                }
                int available = getAvailablePoints(customerId);
                lastEx = new RuntimeException("积分不足，可用积分：" + available);
            } catch (Exception e) {
                lastEx = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
            }
            if (attempt < MAX_RETRIES) {
                sleep(50L * attempt);
            }
        }
        throw lastEx != null ? lastEx : new RuntimeException("积分抵扣失败，请稍后重试");
    }

    @Override
    public BigDecimal calculateDeductAmount(Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal deductRate = pointsConfigService.getDeductRate();
        return BigDecimal.valueOf(points).divide(deductRate, 2, RoundingMode.HALF_UP);
    }

    @Override
    public Integer calculatePointsToDeduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal deductRate = pointsConfigService.getDeductRate();
        return amount.multiply(deductRate).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    @Override
    public Integer calculateEarnedPoints(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal earnRate = pointsConfigService.getEarnRate();
        return amount.multiply(earnRate).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
