package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.DamageRegistration;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.DamageRegistrationService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.RentalOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/damage")
public class DamageRegistrationController {

    @Autowired
    private DamageRegistrationService damageRegistrationService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private CustomerService customerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> toFlatMap(DamageRegistration damage) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(damage, Map.class);
            Instrument instrument = instrumentService.getById(damage.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            RentalOrder order = rentalOrderService.getById(damage.getOrderId());
            map.put("orderNo", order != null ? order.getOrderNo() : "");
            Customer customer = customerService.getById(damage.getCustomerId());
            map.put("customerName", customer != null ? customer.getName() : "");
            return map;
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", damage.getId());
            map.put("orderId", damage.getOrderId());
            map.put("instrumentId", damage.getInstrumentId());
            map.put("customerId", damage.getCustomerId());
            map.put("damageType", damage.getDamageType());
            map.put("description", damage.getDescription());
            map.put("severity", damage.getSeverity());
            map.put("estimatedCost", damage.getEstimatedCost());
            map.put("status", damage.getStatus());
            map.put("remark", damage.getRemark());
            map.put("createTime", damage.getCreateTime());
            Instrument instrument = instrumentService.getById(damage.getInstrumentId());
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            RentalOrder order = rentalOrderService.getById(damage.getOrderId());
            map.put("orderNo", order != null ? order.getOrderNo() : "");
            Customer customer = customerService.getById(damage.getCustomerId());
            map.put("customerName", customer != null ? customer.getName() : "");
            return map;
        }
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(PageQuery pageQuery,
                                                    @RequestParam(required = false) Long instrumentId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String severity) {
        LambdaQueryWrapper<DamageRegistration> wrapper = new LambdaQueryWrapper<>();
        if (instrumentId != null) {
            wrapper.eq(DamageRegistration::getInstrumentId, instrumentId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DamageRegistration::getStatus, status);
        }
        if (severity != null && !severity.isEmpty()) {
            wrapper.eq(DamageRegistration::getSeverity, severity);
        }
        wrapper.orderByDesc(DamageRegistration::getCreateTime);
        Page<DamageRegistration> page = damageRegistrationService.page(pageQuery.toPage(), wrapper);
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(this::toFlatMap).toList());
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        DamageRegistration damage = damageRegistrationService.getById(id);
        if (damage == null) {
            return Result.fail("损坏记录不存在");
        }
        return Result.ok(toFlatMap(damage));
    }

    @PostMapping
    public Result<Void> create(@RequestBody DamageRegistration damage) {
        damage.setStatus("REPORTED");
        damageRegistrationService.save(damage);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody DamageRegistration damage) {
        damageRegistrationService.updateById(damage);
        return Result.ok();
    }
}
