package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.DamageRegistration;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.MaintenanceRecord;
import com.instrument.rental.entity.RepairOrder;
import com.instrument.rental.service.DamageRegistrationService;
import com.instrument.rental.service.DepositRecordService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.MaintenanceRecordService;
import com.instrument.rental.service.RepairOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/repair")
public class RepairOrderController {

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private DamageRegistrationService damageRegistrationService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    @Autowired
    private DepositRecordService depositRecordService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> toFlatMap(RepairOrder repair) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(repair, Map.class);
            Instrument instrument = instrumentService.getById(repair.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            if (repair.getDamageId() != null) {
                DamageRegistration damage = damageRegistrationService.getById(repair.getDamageId());
                map.put("damageDescription", damage != null ? damage.getDescription() : "");
            }
            return map;
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", repair.getId());
            map.put("orderNo", repair.getOrderNo());
            map.put("damageId", repair.getDamageId());
            map.put("instrumentId", repair.getInstrumentId());
            map.put("repairType", repair.getRepairType());
            map.put("description", repair.getDescription());
            map.put("estimatedCost", repair.getEstimatedCost());
            map.put("actualCost", repair.getActualCost());
            map.put("assignee", repair.getAssignee());
            map.put("status", repair.getStatus());
            map.put("maintenanceRecordId", repair.getMaintenanceRecordId());
            map.put("remark", repair.getRemark());
            map.put("createTime", repair.getCreateTime());
            Instrument instrument = instrumentService.getById(repair.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            if (repair.getDamageId() != null) {
                DamageRegistration damage = damageRegistrationService.getById(repair.getDamageId());
                map.put("damageDescription", damage != null ? damage.getDescription() : "");
            }
            return map;
        }
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(PageQuery pageQuery,
                                                    @RequestParam(required = false) Long instrumentId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (instrumentId != null) {
            wrapper.eq(RepairOrder::getInstrumentId, instrumentId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(RepairOrder::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(RepairOrder::getOrderNo, keyword);
        }
        wrapper.orderByDesc(RepairOrder::getCreateTime);
        Page<RepairOrder> page = repairOrderService.page(pageQuery.toPage(), wrapper);
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(this::toFlatMap).toList());
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        RepairOrder repair = repairOrderService.getById(id);
        if (repair == null) {
            return Result.fail("维修工单不存在");
        }
        return Result.ok(toFlatMap(repair));
    }

    @PostMapping
    public Result<Void> create(@RequestBody RepairOrder repairOrder) {
        if (repairOrder.getDamageId() != null) {
            DamageRegistration damage = damageRegistrationService.getById(repairOrder.getDamageId());
            if (damage != null && "REPORTED".equals(damage.getStatus())) {
                damage.setStatus("REPAIR_CREATED");
                damageRegistrationService.updateById(damage);
            }
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "WX" + dateStr;
        long count = this.repairOrderService.count(new LambdaQueryWrapper<RepairOrder>().likeRight(RepairOrder::getOrderNo, prefix));
        String seq = String.format("%03d", count + 1);
        repairOrder.setOrderNo(prefix + seq);

        repairOrder.setStatus("PENDING");
        repairOrderService.save(repairOrder);

        Instrument instrument = instrumentService.getById(repairOrder.getInstrumentId());
        if (instrument != null) {
            instrument.setStatus("MAINTENANCE");
            instrumentService.updateById(instrument);
        }

        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody RepairOrder repairOrder) {
        repairOrderService.updateById(repairOrder);
        return Result.ok();
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody Map<String, Object> params) {
        Long repairId = Long.valueOf(params.get("id").toString());
        RepairOrder repair = repairOrderService.getById(repairId);
        if (repair == null) {
            return Result.fail("维修工单不存在");
        }

        String remark = params.get("remark") != null ? params.get("remark").toString() : "";
        BigDecimal actualCost = params.get("actualCost") != null ? new BigDecimal(params.get("actualCost").toString()) : BigDecimal.ZERO;
        Boolean deductDeposit = params.get("deductDeposit") != null && Boolean.parseBoolean(params.get("deductDeposit").toString());

        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        maintenanceRecord.setInstrumentId(repair.getInstrumentId());
        maintenanceRecord.setType("维修");
        maintenanceRecord.setDescription(repair.getDescription());
        maintenanceRecord.setCost(actualCost.toPlainString());
        maintenanceRecord.setMaintenanceDate(LocalDate.now());
        maintenanceRecord.setStatus("COMPLETED");
        maintenanceRecord.setRemark(remark);
        maintenanceRecordService.save(maintenanceRecord);

        repair.setStatus("COMPLETED");
        repair.setActualCost(actualCost);
        repair.setMaintenanceRecordId(maintenanceRecord.getId());
        repair.setRemark(remark);
        repairOrderService.updateById(repair);

        if (repair.getDamageId() != null) {
            DamageRegistration damage = damageRegistrationService.getById(repair.getDamageId());
            if (damage != null) {
                damage.setStatus("REPAIRED");
                damageRegistrationService.updateById(damage);

                if (deductDeposit && damage.getOrderId() != null) {
                    depositRecordService.deductDeposit(damage.getOrderId(), actualCost, "维修工单[" + repair.getOrderNo() + "]扣款");
                }
            }
        }

        Instrument instrument = instrumentService.getById(repair.getInstrumentId());
        if (instrument != null && "MAINTENANCE".equals(instrument.getStatus())) {
            instrument.setStatus("AVAILABLE");
            instrumentService.updateById(instrument);
        }

        return Result.ok();
    }
}
