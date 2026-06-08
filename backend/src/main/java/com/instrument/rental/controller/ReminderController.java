package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.entity.Reminder;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.RentalOrderService;
import com.instrument.rental.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private CustomerService customerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> toFlatMap(Reminder reminder) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(reminder, Map.class);
            RentalOrder order = rentalOrderService.getById(reminder.getOrderId());
            if (order != null) {
                map.put("orderNo", order.getOrderNo());
                map.put("instrumentId", order.getInstrumentId());
            }
            Customer customer = customerService.getById(reminder.getCustomerId());
            if (customer != null) {
                map.put("customerName", customer.getName());
                map.put("customerPhone", customer.getPhone());
            }
            return map;
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", reminder.getId());
            map.put("orderId", reminder.getOrderId());
            map.put("customerId", reminder.getCustomerId());
            map.put("expireDate", reminder.getExpireDate());
            map.put("daysBeforeExpire", reminder.getDaysBeforeExpire());
            map.put("status", reminder.getStatus());
            map.put("notifyMethod", reminder.getNotifyMethod());
            map.put("notifyTime", reminder.getNotifyTime());
            map.put("createTime", reminder.getCreateTime());
            RentalOrder order = rentalOrderService.getById(reminder.getOrderId());
            if (order != null) {
                map.put("orderNo", order.getOrderNo());
                map.put("instrumentId", order.getInstrumentId());
            }
            Customer customer = customerService.getById(reminder.getCustomerId());
            if (customer != null) {
                map.put("customerName", customer.getName());
                map.put("customerPhone", customer.getPhone());
            }
            return map;
        }
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(PageQuery pageQuery,
                                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Reminder::getStatus, status);
        }
        wrapper.orderByDesc(Reminder::getCreateTime);
        Page<Reminder> reminderPage = reminderService.page(pageQuery.toPage(), wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(reminderPage.getCurrent(), reminderPage.getSize(), reminderPage.getTotal());
        resultPage.setRecords(reminderPage.getRecords().stream().map(this::toFlatMap).toList());
        return Result.ok(resultPage);
    }

    @PostMapping("/check")
    public Result<Void> check() {
        reminderService.checkAndCreateReminders();
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Reminder reminder = reminderService.getById(id);
        if (reminder == null) {
            return Result.fail("提醒不存在");
        }
        reminder.setStatus(status);
        if ("SENT".equals(status)) {
            reminder.setNotifyTime(LocalDateTime.now());
            log.info("提醒id={}已标记为SENT，通知时间={}", id, reminder.getNotifyTime());
        }
        reminderService.updateById(reminder);
        return Result.ok();
    }

    @PostMapping("/notify/{id}")
    public Result<Void> sendNotify(@PathVariable Long id) {
        Reminder reminder = reminderService.getById(id);
        if (reminder == null) {
            return Result.fail("提醒不存在");
        }
        if (!"PENDING".equals(reminder.getStatus())) {
            return Result.fail("只能发送待通知状态的提醒");
        }
        reminder.setStatus("SENT");
        reminder.setNotifyTime(LocalDateTime.now());
        reminderService.updateById(reminder);

        Customer customer = customerService.getById(reminder.getCustomerId());
        RentalOrder order = rentalOrderService.getById(reminder.getOrderId());
        log.info("发送到期提醒: 客户={}, 电话={}, 订单={}, 到期日={}",
                customer != null ? customer.getName() : "未知",
                customer != null ? customer.getPhone() : "未知",
                order != null ? order.getOrderNo() : "未知",
                reminder.getExpireDate());

        return Result.ok();
    }

    @PostMapping("/notify-all")
    public Result<Integer> sendAllPending() {
        List<Reminder> pendingReminders = reminderService.list(
                new LambdaQueryWrapper<Reminder>().eq(Reminder::getStatus, "PENDING")
        );
        int count = 0;
        for (Reminder reminder : pendingReminders) {
            reminder.setStatus("SENT");
            reminder.setNotifyTime(LocalDateTime.now());
            reminderService.updateById(reminder);
            count++;
        }
        log.info("批量发送到期提醒: 共发送{}条", count);
        return Result.ok(count);
    }
}
