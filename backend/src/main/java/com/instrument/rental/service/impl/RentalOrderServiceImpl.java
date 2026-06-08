package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.dto.RenewalDTO;
import com.instrument.rental.dto.RentalOrderDTO;
import com.instrument.rental.dto.ReturnDTO;
import com.instrument.rental.entity.DepositRecord;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.Reminder;
import com.instrument.rental.entity.RenewalRecord;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.mapper.RentalOrderMapper;
import com.instrument.rental.service.DepositRecordService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.ReminderService;
import com.instrument.rental.service.RenewalRecordService;
import com.instrument.rental.service.RentalOrderService;
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

        RentalOrder order = new RentalOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(dto.getCustomerId());
        order.setInstrumentId(dto.getInstrumentId());
        order.setStartDate(dto.getStartDate());
        order.setEndDate(dto.getEndDate());
        order.setDailyRent(instrument.getDailyRent());
        order.setTotalRent(totalRent);
        order.setDepositAmount(instrument.getDepositAmount());
        order.setStatus("ACTIVE");
        order.setRemark(dto.getRemark());
        this.save(order);

        DepositRecord depositRecord = new DepositRecord();
        depositRecord.setOrderId(order.getId());
        depositRecord.setType("COLLECT");
        depositRecord.setAmount(instrument.getDepositAmount());
        depositRecord.setPayMethod(dto.getPayMethod());
        depositRecord.setStatus("PAID");
        depositRecordService.save(depositRecord);

        instrument.setStatus("RENTED");
        instrumentService.updateById(instrument);

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

        return order;
    }

    @Override
    @Transactional
    public RentalOrder returnOrder(ReturnDTO dto) {
        RentalOrder order = this.getById(dto.getOrderId());

        LocalDate today = LocalDate.now();
        order.setActualReturnDate(today);

        if (today.isAfter(order.getEndDate())) {
            long overdueDays = ChronoUnit.DAYS.between(order.getEndDate(), today);
            BigDecimal overdueFee = order.getDailyRent().multiply(BigDecimal.valueOf(overdueDays)).multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
            order.setOverdueFee(overdueFee);
        }

        if (dto.getDeductAmount() != null && dto.getDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRecord deductRecord = new DepositRecord();
            deductRecord.setOrderId(order.getId());
            deductRecord.setType("DEDUCT");
            deductRecord.setAmount(dto.getDeductAmount());
            deductRecord.setStatus("PAID");
            depositRecordService.save(deductRecord);
        }

        BigDecimal deducted = dto.getDeductAmount() != null ? dto.getDeductAmount() : BigDecimal.ZERO;
        BigDecimal refundAmount = order.getDepositAmount().subtract(deducted);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            DepositRecord refundRecord = new DepositRecord();
            refundRecord.setOrderId(order.getId());
            refundRecord.setType("REFUND");
            refundRecord.setAmount(refundAmount);
            refundRecord.setPayMethod(dto.getRefundMethod());
            refundRecord.setStatus("PAID");
            depositRecordService.save(refundRecord);
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

        return order;
    }
}
