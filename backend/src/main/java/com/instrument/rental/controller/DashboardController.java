package com.instrument.rental.controller;

import com.instrument.rental.common.Result;
import com.instrument.rental.dto.DashboardVO;
import com.instrument.rental.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        DashboardVO dashboardVO = reminderService.getDashboardStats();
        return Result.ok(dashboardVO);
    }
}
