package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.DepositRecord;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.service.DepositRecordService;
import com.instrument.rental.service.RentalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposit")
public class DepositRecordController {

    @Autowired
    private DepositRecordService depositRecordService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @GetMapping("/list")
    public Result<Page<DepositRecord>> list(PageQuery pageQuery,
                                             @RequestParam(required = false) String orderId,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status) {
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null && !orderId.isEmpty()) {
            try {
                wrapper.eq(DepositRecord::getOrderId, Long.parseLong(orderId));
            } catch (NumberFormatException ignored) {
            }
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(DepositRecord::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DepositRecord::getStatus, status);
        }
        wrapper.orderByDesc(DepositRecord::getCreateTime);
        Page<DepositRecord> page = depositRecordService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
    }

    @PostMapping
    public Result<Void> create(@RequestBody DepositRecord depositRecord) {
        depositRecordService.save(depositRecord);
        return Result.ok();
    }
}
