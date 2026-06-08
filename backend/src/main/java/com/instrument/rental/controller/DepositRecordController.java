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
                                             @RequestParam(required = false) Long orderId,
                                             @RequestParam(required = false) String type) {
        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null) {
            wrapper.eq(DepositRecord::getOrderId, orderId);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(DepositRecord::getType, type);
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
