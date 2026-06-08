package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.entity.Reminder;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.RentalOrderService;
import com.instrument.rental.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private CustomerService customerService;

    @GetMapping("/list")
    public Result<Page<Reminder>> list(PageQuery pageQuery,
                                        @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Reminder::getStatus, status);
        }
        wrapper.orderByDesc(Reminder::getCreateTime);
        Page<Reminder> page = reminderService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
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
        reminderService.updateById(reminder);
        return Result.ok();
    }
}
