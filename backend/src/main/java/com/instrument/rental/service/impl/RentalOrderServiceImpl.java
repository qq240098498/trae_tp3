package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.dto.CouponCalculateVO;
import com.instrument.rental.dto.PointsCalculateVO;
import com.instrument.rental.dto.RenewalDTO;
import com.instrument.rental.dto.RentalOrderDTO;
import com.instrument.rental.dto.ReturnDTO;
import com.instrument.rental.entity.Coupon;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.Reminder;
import com.instrument.rental.entity.RenewalRecord;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.mapper.RentalOrderMapper;
import com.instrument.rental.service.CouponService;
import com.instrument.rental.service.CustomerPointsService;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.DepositRecordService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.PointsConfigService;
import com.instrument.rental.service.ReminderService;
import com.instrument.rental.service.RenewalRecordService;
import com.instrument.rental.service.RentalOrderService;
import com.instrument.rental.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
public class RentalOrderServiceImpl extends ServiceImpl<RentalOrderMapper, RentalOrder> implements RentalOrderService {

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private DepositRecordService depositRecordService;

    @Autowired
    private RenewalRecordService renewalRecordService;

    @Lazy
    @Autowired
    private ReminderService reminderService;

    @Autowired
    private CustomerPointsService customerPointsService;

    @Autowired
    private PointsConfigService pointsConfigService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private WorkLogService workLogService;

    @Override
    @Transactional
    public RentalOrder createOrder(RentalOrderDTO dto) {
        Instrument instrument = instrumentService.getById(dto.getInstrumentId());

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "RL" + dateStr;
        long count = this.count(new LambdaQueryWrapper<RentalOrder>().likeRight(RentalOrder::getOrderNo, prefix));
        String seq = String.format("%03d", count + 1);
        String orderNo = prefix + seq;

        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
        BigDecimal totalRent = instrument.getDailyRent().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

        Coupon coupon = null;
        BigDecimal couponDeductAmount = BigDecimal.ZERO;
        Long couponId = null;
        boolean canUsePoints = true;

        if (dto.getUseCouponId() != null) {
            coupon = couponService.getCouponById(dto.getUseCouponId());
            if (coupon != null) {
                if (!coupon.getCustomerId().equals(dto.getCustomerId())) {
                    throw new RuntimeException("优惠券不属于当前客户");
                }
                if (!couponService.checkCouponAvailable(coupon, totalRent)) {
                    throw new RuntimeException("优惠券不可用或不满足最低使用条件");
                }
                couponDeductAmount = couponService.calculateDiscount(coupon, totalRent);
                couponId = coupon.getId();
                if (coupon.getPointsCompatible() != null && !coupon.getPointsCompatible()) {
                    canUsePoints = false;
                }
            }
        }

        BigDecimal pointsDeductAmount = BigDecimal.ZERO;
        Integer usedPoints = 0;
        BigDecimal actualPayAmount = totalRent.subtract(couponDeductAmount).setScale(2, RoundingMode.HALF_UP);
        if (actualPayAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualPayAmount = BigDecimal.ZERO;
        }

        if (canUsePoints && Boolean.TRUE.equals(dto.getUsePoints()) && dto.getUsePointsAmount() != null && dto.getUsePointsAmount() > 0) {
            int availablePoints = customerPointsService.getAvailablePoints(dto.getCustomerId());
            int pointsToUse = Math.min(dto.getUsePointsAmount(), availablePoints);

            BigDecimal maxDeductPercent = pointsConfigService.getMaxDeductPercent();
            BigDecimal maxDeductAmount = totalRent.multiply(maxDeductPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal tempDeductAmount = customerPointsService.calculateDeductAmount(pointsToUse);
            if (tempDeductAmount.compareTo(maxDeductAmount) > 0) {
                tempDeductAmount = maxDeductAmount;
                pointsToUse = customerPointsService.calculatePointsToDeduct(tempDeductAmount);
            }

            if (pointsToUse > 0 && tempDeductAmount.compareTo(BigDecimal.ZERO) > 0) {
                pointsDeductAmount = tempDeductAmount;
                usedPoints = pointsToUse;
                actualPayAmount = totalRent.subtract(couponDeductAmount).subtract(pointsDeductAmount).setScale(2, RoundingMode.HALF_UP);
                if (actualPayAmount.compareTo(BigDecimal.ZERO) < 0) {
                    actualPayAmount = BigDecimal.ZERO;
                }
            }
        } else if (!canUsePoints && Boolean.TRUE.equals(dto.getUsePoints())) {
            throw new RuntimeException("当前优惠券与积分抵扣互斥，无法同时使用");
        }

        RentalOrder order = new RentalOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(dto.getCustomerId());
        order.setInstrumentId(dto.getInstrumentId());
        order.setStartDate(dto.getStartDate());
        order.setEndDate(dto.getEndDate());
        order.setDailyRent(instrument.getDailyRent());
        order.setTotalRent(totalRent);
        order.setDepositAmount(instrument.getDepositAmount());
        order.setPointsDeductAmount(pointsDeductAmount);
        order.setUsedPoints(usedPoints);
        order.setCouponId(couponId);
        order.setCouponDeductAmount(couponDeductAmount);
        order.setActualPayAmount(actualPayAmount);
        order.setStatus("ACTIVE");
        order.setRemark(dto.getRemark());
        this.save(order);

        if (coupon != null && couponId != null) {
            couponService.useCoupon(couponId, order.getId(), totalRent);
        }

        if (usedPoints > 0) {
            customerPointsService.deductPoints(dto.getCustomerId(), order.getId(), usedPoints, pointsDeductAmount, dto.getOperator(), "租赁订单积分抵扣");
        }

        depositRecordService.collectDeposit(order.getId(), instrument.getDepositAmount(), dto.getPayMethod(), dto.getOperator(), null);

        instrument.setStatus("RENTED");
        instrumentService.updateById(instrument);

        Customer customer = customerService.getById(dto.getCustomerId());
        boolean useDiscount = (couponId != null) || (usedPoints > 0);
        String discountType = null;
        if (couponId != null && usedPoints > 0) {
            discountType = "BOTH";
        } else if (couponId != null) {
            discountType = "COUPON";
        } else if (usedPoints > 0) {
            discountType = "POINTS";
        }
        workLogService.logRentalCreate(
                order.getId(),
                order.getOrderNo(),
                dto.getCustomerId(),
                customer != null ? customer.getName() : null,
                dto.getInstrumentId(),
                instrument.getName(),
                totalRent,
                instrument.getDepositAmount(),
                actualPayAmount,
                useDiscount,
                discountType,
                couponDeductAmount,
                couponId,
                pointsDeductAmount,
                usedPoints,
                dto.getPayMethod(),
                dto.getOperator(),
                dto.getRemark()
        );

        return order;
    }

    @Override
    @Transactional
    public RentalOrder renewOrder(RenewalDTO dto) {
        RentalOrder order = this.getById(dto.getOrderId());
        if (!"ACTIVE".equals(order.getStatus())) {
            throw new RuntimeException("Only ACTIVE orders can be renewed");
        }

        Instrument instrument = instrumentService.getById(order.getInstrumentId());

        long additionalDays = ChronoUnit.DAYS.between(order.getEndDate(), dto.getNewEndDate());
        BigDecimal additionalRent = instrument.getDailyRent().multiply(BigDecimal.valueOf(additionalDays)).setScale(2, RoundingMode.HALF_UP);

        RenewalRecord renewalRecord = new RenewalRecord();
        renewalRecord.setOrderId(order.getId());
        renewalRecord.setOriginalEndDate(order.getEndDate());
        renewalRecord.setNewEndDate(dto.getNewEndDate());
        renewalRecord.setAdditionalRent(additionalRent);
        renewalRecord.setRemark(dto.getRemark());
        renewalRecordService.save(renewalRecord);

        order.setEndDate(dto.getNewEndDate());
        order.setTotalRent(order.getTotalRent().add(additionalRent).setScale(2, RoundingMode.HALF_UP));
        this.updateById(order);

        Reminder existingReminder = reminderService.getOne(
                new LambdaQueryWrapper<Reminder>()
                        .eq(Reminder::getOrderId, order.getId())
                        .eq(Reminder::getStatus, "PENDING")
        );
        if (existingReminder != null) {
            existingReminder.setExpireDate(dto.getNewEndDate());
            existingReminder.setDaysBeforeExpire((int) ChronoUnit.DAYS.between(LocalDate.now(), dto.getNewEndDate()));
            reminderService.updateById(existingReminder);
        }

        Customer customer = customerService.getById(order.getCustomerId());
        workLogService.logRentalRenew(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customer != null ? customer.getName() : null,
                order.getInstrumentId(),
                instrument.getName(),
                additionalRent,
                dto.getOperator(),
                dto.getRemark()
        );

        return order;
    }

    @Override
    @Transactional
    public RentalOrder returnOrder(ReturnDTO dto) {
        RentalOrder order = this.getById(dto.getOrderId());

        LocalDate today = LocalDate.now();
        order.setActualReturnDate(today);

        BigDecimal totalCharge = order.getTotalRent();
        if (today.isAfter(order.getEndDate())) {
            long overdueDays = ChronoUnit.DAYS.between(order.getEndDate(), today);
            BigDecimal overdueFee = order.getDailyRent().multiply(BigDecimal.valueOf(overdueDays)).multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
            order.setOverdueFee(overdueFee);
            totalCharge = totalCharge.add(overdueFee);
        }

        Integer earnedPoints = customerPointsService.calculateEarnedPoints(totalCharge);
        order.setEarnedPoints(earnedPoints);

        if (dto.getDeductAmount() != null && dto.getDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            depositRecordService.deductDeposit(order.getId(), dto.getDeductAmount(), dto.getOperator(), "归还时扣除押金");
        }

        BigDecimal deducted = dto.getDeductAmount() != null ? dto.getDeductAmount() : BigDecimal.ZERO;
        BigDecimal refundAmount = order.getDepositAmount().subtract(deducted);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            depositRecordService.refundDeposit(order.getId(), refundAmount, dto.getRefundMethod(), dto.getOperator(), null);
        }

        Instrument instrument = instrumentService.getById(order.getInstrumentId());
        instrument.setStatus("AVAILABLE");
        if (dto.getInstrumentCondition() != null) {
            instrument.setCond(dto.getInstrumentCondition());
        }
        instrumentService.updateById(instrument);

        order.setStatus("RETURNED");
        if (dto.getRemark() != null) {
            order.setRemark(dto.getRemark());
        }
        this.updateById(order);

        if (earnedPoints > 0) {
            customerPointsService.addPoints(order.getCustomerId(), order.getId(), earnedPoints, totalCharge, dto.getOperator(), "租赁订单消费获得积分");
        }

        Customer customer = customerService.getById(order.getCustomerId());
        workLogService.logRentalReturn(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                customer != null ? customer.getName() : null,
                order.getInstrumentId(),
                instrument != null ? instrument.getName() : null,
                totalCharge,
                order.getOverdueFee(),
                deducted,
                refundAmount,
                earnedPoints,
                dto.getOperator(),
                dto.getRemark()
        );

        return order;
    }

    @Override
    public PointsCalculateVO calculatePoints(Long customerId, Long instrumentId, LocalDate startDate, LocalDate endDate, Integer usePoints) {
        PointsCalculateVO vo = new PointsCalculateVO();

        Instrument instrument = instrumentService.getById(instrumentId);
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal totalRent = instrument.getDailyRent().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

        int availablePoints = customerPointsService.getAvailablePoints(customerId);
        BigDecimal earnRate = pointsConfigService.getEarnRate();
        BigDecimal deductRate = pointsConfigService.getDeductRate();
        BigDecimal maxDeductPercent = pointsConfigService.getMaxDeductPercent();

        BigDecimal availableDeductAmount = customerPointsService.calculateDeductAmount(availablePoints);
        BigDecimal maxDeductAmount = totalRent.multiply(maxDeductPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (availableDeductAmount.compareTo(maxDeductAmount) > 0) {
            availableDeductAmount = maxDeductAmount;
        }

        vo.setAvailablePoints(availablePoints);
        vo.setAvailableDeductAmount(availableDeductAmount);
        vo.setTotalRent(totalRent);
        vo.setEarnRate(earnRate);
        vo.setDeductRate(deductRate);
        vo.setMaxDeductPercent(maxDeductPercent);
        vo.setMaxDeductAmount(maxDeductAmount);

        int pointsToUse = 0;
        BigDecimal deductAmount = BigDecimal.ZERO;
        if (usePoints != null && usePoints > 0) {
            pointsToUse = Math.min(usePoints, availablePoints);
            BigDecimal tempDeduct = customerPointsService.calculateDeductAmount(pointsToUse);
            if (tempDeduct.compareTo(maxDeductAmount) > 0) {
                tempDeduct = maxDeductAmount;
                pointsToUse = customerPointsService.calculatePointsToDeduct(tempDeduct);
            }
            deductAmount = tempDeduct;
        }

        vo.setUsePoints(pointsToUse);
        vo.setDeductAmount(deductAmount);
        vo.setActualPayAmount(totalRent.subtract(deductAmount).setScale(2, RoundingMode.HALF_UP));

        Integer willEarnPoints = customerPointsService.calculateEarnedPoints(totalRent);
        vo.setWillEarnPoints(willEarnPoints);

        return vo;
    }

    @Override
    public CouponCalculateVO calculateWithCoupon(Long customerId, Long instrumentId, LocalDate startDate, LocalDate endDate,
                                                 Long couponId, Integer usePoints) {
        CouponCalculateVO vo = new CouponCalculateVO();

        Instrument instrument = instrumentService.getById(instrumentId);
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal totalRent = instrument.getDailyRent().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        vo.setTotalRent(totalRent);

        var availableCoupons = couponService.getCustomerAvailableCouponsForAmount(customerId, totalRent);
        vo.setAvailableCoupons(availableCoupons);

        int availablePoints = customerPointsService.getAvailablePoints(customerId);
        BigDecimal earnRate = pointsConfigService.getEarnRate();
        BigDecimal deductRate = pointsConfigService.getDeductRate();
        BigDecimal maxDeductPercent = pointsConfigService.getMaxDeductPercent();

        BigDecimal availableDeductAmount = customerPointsService.calculateDeductAmount(availablePoints);
        BigDecimal maxDeductAmount = totalRent.multiply(maxDeductPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (availableDeductAmount.compareTo(maxDeductAmount) > 0) {
            availableDeductAmount = maxDeductAmount;
        }

        vo.setAvailablePoints(availablePoints);
        vo.setAvailableDeductAmount(availableDeductAmount);
        vo.setEarnRate(earnRate);
        vo.setDeductRate(deductRate);
        vo.setMaxDeductPercent(maxDeductPercent);
        vo.setMaxDeductAmount(maxDeductAmount);

        Coupon selectedCoupon = null;
        BigDecimal couponDeductAmount = BigDecimal.ZERO;
        boolean canUsePoints = true;

        if (couponId != null) {
            Coupon coupon = couponService.getCouponById(couponId);
            if (coupon != null && coupon.getCustomerId().equals(customerId)
                    && couponService.checkCouponAvailable(coupon, totalRent)) {
                selectedCoupon = coupon;
                couponDeductAmount = couponService.calculateDiscount(coupon, totalRent);
                if (coupon.getPointsCompatible() != null && !coupon.getPointsCompatible()) {
                    canUsePoints = false;
                }
            } else {
                vo.setMessage("所选优惠券不可用或不满足最低使用条件");
            }
        }

        vo.setSelectedCoupon(selectedCoupon);
        vo.setCouponDeductAmount(couponDeductAmount);
        vo.setPointsCompatible(canUsePoints);

        int pointsToUse = 0;
        BigDecimal pointsDeductAmount = BigDecimal.ZERO;
        if (canUsePoints && usePoints != null && usePoints > 0) {
            pointsToUse = Math.min(usePoints, availablePoints);
            BigDecimal tempDeduct = customerPointsService.calculateDeductAmount(pointsToUse);
            if (tempDeduct.compareTo(maxDeductAmount) > 0) {
                tempDeduct = maxDeductAmount;
                pointsToUse = customerPointsService.calculatePointsToDeduct(tempDeduct);
            }
            pointsDeductAmount = tempDeduct;
        } else if (!canUsePoints && usePoints != null && usePoints > 0) {
            vo.setMessage("当前优惠券与积分抵扣互斥，已自动禁用积分抵扣");
        }

        vo.setUsePoints(pointsToUse);
        vo.setPointsDeductAmount(pointsDeductAmount);

        BigDecimal actualPay = totalRent.subtract(couponDeductAmount).subtract(pointsDeductAmount).setScale(2, RoundingMode.HALF_UP);
        if (actualPay.compareTo(BigDecimal.ZERO) < 0) {
            actualPay = BigDecimal.ZERO;
        }
        vo.setActualPayAmount(actualPay);

        Integer willEarnPoints = customerPointsService.calculateEarnedPoints(totalRent);
        vo.setWillEarnPoints(willEarnPoints);

        return vo;
    }
}
