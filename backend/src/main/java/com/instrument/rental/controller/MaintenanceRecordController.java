package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.MaintenanceRecord;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.MaintenanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRecordController {

    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    @Autowired
    private InstrumentService instrumentService;

    @GetMapping("/list")
    public Result<Page<MaintenanceRecord>> list(PageQuery pageQuery,
                                                 @RequestParam(required = false) Long instrumentId,
                                                 @RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String status) {
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (instrumentId != null) {
            wrapper.eq(MaintenanceRecord::getInstrumentId, instrumentId);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(MaintenanceRecord::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MaintenanceRecord::getStatus, status);
        }
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        Page<MaintenanceRecord> page = maintenanceRecordService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<MaintenanceRecord> getById(@PathVariable Long id) {
        MaintenanceRecord record = maintenanceRecordService.getById(id);
        if (record == null) {
            return Result.fail("维护记录不存在");
        }
        return Result.ok(record);
    }

    @PostMapping
    public Result<Void> create(@RequestBody MaintenanceRecord maintenanceRecord) {
        maintenanceRecordService.save(maintenanceRecord);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody MaintenanceRecord maintenanceRecord) {
        maintenanceRecordService.updateById(maintenanceRecord);
        return Result.ok();
    }
}
