package com.instrument.rental.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.WorkLog;
import com.instrument.rental.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-log")
public class WorkLogController {

    @Autowired
    private WorkLogService workLogService;

    @GetMapping("/list")
    public Result<Page<WorkLog>> list(PageQuery pageQuery,
                                      @RequestParam(required = false) String processType,
                                      @RequestParam(required = false) Long customerId,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) String operator) {
        Page<WorkLog> page = workLogService.pageQuery(pageQuery, processType, customerId, orderId, operator);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<WorkLog> getById(@PathVariable Long id) {
        WorkLog log = workLogService.getById(id);
        return Result.ok(log);
    }

    @GetMapping("/order/{orderId}")
    public Result<List<WorkLog>> getByOrderId(@PathVariable Long orderId) {
        List<WorkLog> list = workLogService.getByOrderId(orderId);
        return Result.ok(list);
    }

    @GetMapping("/customer/{customerId}")
    public Result<List<WorkLog>> getByCustomerId(@PathVariable Long customerId) {
        List<WorkLog> list = workLogService.getByCustomerId(customerId);
        return Result.ok(list);
    }

    @GetMapping("/process-types")
    public Result<List<String>> getProcessTypes() {
        List<String> types = List.of(
                "RENTAL_CREATE", "RENTAL_RENEW", "RENTAL_RETURN",
                "COUPON_ISSUE", "COUPON_USE", "COUPON_REVOKE",
                "DEPOSIT_COLLECT", "DEPOSIT_REFUND", "DEPOSIT_DEDUCT",
                "POINTS_DEDUCT", "POINTS_EARN"
        );
        return Result.ok(types);
    }
}
