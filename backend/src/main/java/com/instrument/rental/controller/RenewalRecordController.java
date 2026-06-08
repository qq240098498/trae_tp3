package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.RenewalRecord;
import com.instrument.rental.service.RenewalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/renewal")
public class RenewalRecordController {

    @Autowired
    private RenewalRecordService renewalRecordService;

    @GetMapping("/list")
    public Result<Page<RenewalRecord>> list(PageQuery pageQuery,
                                             @RequestParam(required = false) Long orderId) {
        LambdaQueryWrapper<RenewalRecord> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null) {
            wrapper.eq(RenewalRecord::getOrderId, orderId);
        }
        wrapper.orderByDesc(RenewalRecord::getCreateTime);
        Page<RenewalRecord> page = renewalRecordService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
    }

    @GetMapping("/order/{orderId}")
    public Result<List<RenewalRecord>> listByOrder(@PathVariable Long orderId) {
        LambdaQueryWrapper<RenewalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RenewalRecord::getOrderId, orderId);
        wrapper.orderByDesc(RenewalRecord::getCreateTime);
        List<RenewalRecord> list = renewalRecordService.list(wrapper);
        return Result.ok(list);
    }
}
