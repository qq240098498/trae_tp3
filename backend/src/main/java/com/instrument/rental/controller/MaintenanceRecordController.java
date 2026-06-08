package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.MaintenanceRecord;
import com.instrument.rental.entity.RepairOrder;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.MaintenanceRecordService;
import com.instrument.rental.service.RepairOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRecordController {

    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private RepairOrderService repairOrderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> toFlatMap(MaintenanceRecord record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(record, Map.class);
            Instrument instrument = instrumentService.getById(record.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            return map;
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("instrumentId", record.getInstrumentId());
            map.put("type", record.getType());
            map.put("description", record.getDescription());
            map.put("cost", record.getCost());
            map.put("maintenanceDate", record.getMaintenanceDate());
            map.put("status", record.getStatus());
            map.put("remark", record.getRemark());
            map.put("createTime", record.getCreateTime());
            Instrument instrument = instrumentService.getById(record.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            return map;
        }
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(PageQuery pageQuery,
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
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(r -> {
            Map<String, Object> map = toFlatMap(r);
            LambdaQueryWrapper<RepairOrder> repairWrapper = new LambdaQueryWrapper<>();
            repairWrapper.eq(RepairOrder::getMaintenanceRecordId, r.getId());
            RepairOrder repairOrder = repairOrderService.getOne(repairWrapper, false);
            if (repairOrder != null) {
                map.put("repairOrderNo", repairOrder.getOrderNo());
                map.put("repairOrderId", repairOrder.getId());
            }
            return map;
        }).toList());
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        MaintenanceRecord record = maintenanceRecordService.getById(id);
        if (record == null) {
            return Result.fail("维护记录不存在");
        }
        Map<String, Object> map = toFlatMap(record);
        LambdaQueryWrapper<RepairOrder> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(RepairOrder::getMaintenanceRecordId, id);
        RepairOrder repairOrder = repairOrderService.getOne(repairWrapper, false);
        if (repairOrder != null) {
            map.put("repairOrderNo", repairOrder.getOrderNo());
            map.put("repairOrderId", repairOrder.getId());
        }
        return Result.ok(map);
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
