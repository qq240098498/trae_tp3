package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.dto.DashboardVO;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.entity.Reminder;
import com.instrument.rental.mapper.ReminderMapper;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.RentalOrderService;
import com.instrument.rental.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder> implements ReminderService {

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Override
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkAndCreateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        List<RentalOrder> activeOrders = rentalOrderService.list(
                new LambdaQueryWrapper<RentalOrder>()
                        .eq(RentalOrder::getStatus, "ACTIVE")
                        .le(RentalOrder::getEndDate, sevenDaysLater)
                        .ge(RentalOrder::getEndDate, today)
        );

        for (RentalOrder order : activeOrders) {
            long existingCount = this.count(
                    new LambdaQueryWrapper<Reminder>()
                            .eq(Reminder::getOrderId, order.getId())
                            .eq(Reminder::getStatus, "PENDING")
            );
            if (existingCount > 0) {
                continue;
            }

            int daysBeforeExpire = (int) ChronoUnit.DAYS.between(today, order.getEndDate());

            Reminder reminder = new Reminder();
            reminder.setOrderId(order.getId());
            reminder.setCustomerId(order.getCustomerId());
            reminder.setExpireDate(order.getEndDate());
            reminder.setDaysBeforeExpire(daysBeforeExpire);
            reminder.setStatus("PENDING");
            reminder.setNotifyMethod("SYSTEM");
            this.save(reminder);
        }

        List<RentalOrder> overdueOrders = rentalOrderService.list(
                new LambdaQueryWrapper<RentalOrder>()
                        .eq(RentalOrder::getStatus, "ACTIVE")
                        .lt(RentalOrder::getEndDate, today)
        );

        for (RentalOrder order : overdueOrders) {
            order.setStatus("OVERDUE");
            rentalOrderService.updateById(order);
        }
    }

    @Override
    public DashboardVO getDashboardStats() {
        DashboardVO vo = new DashboardVO();

        vo.setTotalInstruments(instrumentService.count());
        vo.setAvailableInstruments(instrumentService.count(
                new LambdaQueryWrapper<Instrument>().eq(Instrument::getStatus, "AVAILABLE")
        ));
        vo.setRentedInstruments(instrumentService.count(
                new LambdaQueryWrapper<Instrument>().eq(Instrument::getStatus, "RENTED")
        ));
        vo.setTotalCustomers(customerService.count());
        vo.setActiveOrders(rentalOrderService.count(
                new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getStatus, "ACTIVE")
        ));
        vo.setOverdueOrders(rentalOrderService.count(
                new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getStatus, "OVERDUE")
        ));
        vo.setPendingReminders(this.count(
                new LambdaQueryWrapper<Reminder>().eq(Reminder::getStatus, "PENDING")
        ));

        return vo;
    }
}
