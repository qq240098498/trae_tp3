package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.dto.DashboardVO;
import com.instrument.rental.entity.Reminder;

public interface ReminderService extends IService<Reminder> {

    void checkAndCreateReminders();

    DashboardVO getDashboardStats();
}
