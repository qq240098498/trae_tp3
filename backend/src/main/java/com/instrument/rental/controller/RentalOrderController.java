package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.dto.RenewalDTO;
import com.instrument.rental.dto.RentalOrderDTO;
import com.instrument.rental.dto.ReturnDTO;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.entity.RentalOrder;
import com.instrument.rental.service.CustomerService;
import com.instrument.rental.service.InstrumentService;
import com.instrument.rental.service.RentalOrderService;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class RentalOrderController {

    @Autowired
    private RentalOrderService rentalOrderService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private CustomerService customerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> toFlatMap(RentalOrder order) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(order, Map.class);
            Customer customer = customerService.getById(order.getCustomerId());
            Instrument instrument = instrumentService.getById(order.getInstrumentId());
            map.put("customerName", customer != null ? customer.getName() : "");
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            return map;
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("customerId", order.getCustomerId());
            map.put("instrumentId", order.getInstrumentId());
            map.put("startDate", order.getStartDate());
            map.put("endDate", order.getEndDate());
            map.put("dailyRent", order.getDailyRent());
            map.put("totalRent", order.getTotalRent());
            map.put("depositAmount", order.getDepositAmount());
            map.put("pointsDeductAmount", order.getPointsDeductAmount());
            map.put("usedPoints", order.getUsedPoints());
            map.put("actualPayAmount", order.getActualPayAmount());
            map.put("earnedPoints", order.getEarnedPoints());
            map.put("status", order.getStatus());
            map.put("actualReturnDate", order.getActualReturnDate());
            map.put("overdueFee", order.getOverdueFee());
            map.put("remark", order.getRemark());
            map.put("createTime", order.getCreateTime());
            Customer customer = customerService.getById(order.getCustomerId());
            Instrument instrument = instrumentService.getById(order.getInstrumentId());
            map.put("customerName", customer != null ? customer.getName() : "");
            map.put("instrumentName", instrument != null ? instrument.getName() : "");
            return map;
        }
    }

    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(PageQuery pageQuery,
                                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageQuery.getKeyword())) {
            wrapper.like(RentalOrder::getOrderNo, pageQuery.getKeyword());
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(RentalOrder::getStatus, status);
        }
        wrapper.orderByDesc(RentalOrder::getCreateTime);
        Page<RentalOrder> orderPage = rentalOrderService.page(pageQuery.toPage(), wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        resultPage.setRecords(orderPage.getRecords().stream().map(this::toFlatMap).toList());
        return Result.ok(resultPage);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        RentalOrder order = rentalOrderService.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.ok(toFlatMap(order));
    }

    @PostMapping
    public Result<RentalOrder> create(@RequestBody RentalOrderDTO dto) {
        RentalOrder order = rentalOrderService.createOrder(dto);
        return Result.ok(order);
    }

    @PostMapping("/renew")
    public Result<RentalOrder> renew(@RequestBody RenewalDTO dto) {
        RentalOrder order = rentalOrderService.renewOrder(dto);
        return Result.ok(order);
    }

    @PostMapping("/return")
    public Result<RentalOrder> returnOrder(@RequestBody ReturnDTO dto) {
        RentalOrder order = rentalOrderService.returnOrder(dto);
        return Result.ok(order);
    }

    @GetMapping("/calculate-points")
    public Result<com.instrument.rental.dto.PointsCalculateVO> calculatePoints(
            @RequestParam Long customerId,
            @RequestParam Long instrumentId,
            @RequestParam java.time.LocalDate startDate,
            @RequestParam java.time.LocalDate endDate,
            @RequestParam(required = false) Integer usePoints) {
        com.instrument.rental.dto.PointsCalculateVO vo = rentalOrderService.calculatePoints(customerId, instrumentId, startDate, endDate, usePoints);
        return Result.ok(vo);
    }
}
