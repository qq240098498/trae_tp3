package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Coupon;
import com.instrument.rental.entity.CouponRecord;
import com.instrument.rental.entity.CouponTemplate;
import com.instrument.rental.service.CouponRecordService;
import com.instrument.rental.service.CouponService;
import com.instrument.rental.service.CouponTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRecordService couponRecordService;

    @GetMapping("/templates")
    public Result<List<CouponTemplate>> getTemplates() {
        List<CouponTemplate> list = couponTemplateService.getAllTemplates();
        return Result.ok(list);
    }

    @GetMapping("/templates/active")
    public Result<List<CouponTemplate>> getActiveTemplates() {
        List<CouponTemplate> list = couponTemplateService.getActiveTemplates();
        return Result.ok(list);
    }

    @GetMapping("/templates/{id}")
    public Result<CouponTemplate> getTemplateById(@PathVariable Long id) {
        CouponTemplate template = couponTemplateService.getTemplateById(id);
        return Result.ok(template);
    }

    @PostMapping("/templates")
    public Result<CouponTemplate> createTemplate(@RequestBody CouponTemplate template) {
        CouponTemplate created = couponTemplateService.createTemplate(template);
        return Result.ok(created);
    }

    @PutMapping("/templates")
    public Result<CouponTemplate> updateTemplate(@RequestBody CouponTemplate template) {
        CouponTemplate updated = couponTemplateService.updateTemplate(template);
        return Result.ok(updated);
    }

    @DeleteMapping("/templates/{id}")
    public Result<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        boolean success = couponTemplateService.deleteTemplate(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return Result.ok(result);
    }

    @PostMapping("/issue")
    public Result<Map<String, Object>> issueCoupon(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(params.get("templateId").toString());
        Long customerId = Long.valueOf(params.get("customerId").toString());
        String operator = params.get("operator") != null ? params.get("operator").toString() : null;
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;

        Coupon coupon = couponService.issueCoupon(templateId, customerId, operator, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("coupon", coupon);
        result.put("success", true);
        return Result.ok(result);
    }

    @PostMapping("/issue/batch")
    public Result<Map<String, Object>> issueCouponsBatch(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(params.get("templateId").toString());
        List<Long> customerIds = (List<Long>) params.get("customerIds");
        String operator = params.get("operator") != null ? params.get("operator").toString() : null;
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;

        List<Coupon> coupons = couponService.issueCouponsBatch(templateId, customerIds, operator, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("coupons", coupons);
        result.put("count", coupons.size());
        result.put("success", true);
        return Result.ok(result);
    }

    @GetMapping("/customer/{customerId}")
    public Result<List<Coupon>> getCustomerCoupons(@PathVariable Long customerId) {
        List<Coupon> list = couponService.getCustomerCoupons(customerId);
        return Result.ok(list);
    }

    @GetMapping("/customer/{customerId}/available")
    public Result<List<Coupon>> getCustomerAvailableCoupons(@PathVariable Long customerId) {
        List<Coupon> list = couponService.getCustomerAvailableCoupons(customerId);
        return Result.ok(list);
    }

    @GetMapping("/customer/{customerId}/available-for-amount")
    public Result<List<Coupon>> getCustomerAvailableCouponsForAmount(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {
        List<Coupon> list = couponService.getCustomerAvailableCouponsForAmount(customerId, amount);
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<Coupon> getCouponById(@PathVariable Long id) {
        Coupon coupon = couponService.getCouponById(id);
        return Result.ok(coupon);
    }

    @GetMapping("/no/{couponNo}")
    public Result<Coupon> getCouponByNo(@PathVariable String couponNo) {
        Coupon coupon = couponService.getCouponByNo(couponNo);
        return Result.ok(coupon);
    }

    @PostMapping("/{id}/revoke")
    public Result<Map<String, Object>> revokeCoupon(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        String operator = params != null && params.get("operator") != null ? params.get("operator").toString() : null;
        String remark = params != null && params.get("remark") != null ? params.get("remark").toString() : null;

        boolean success = couponService.revokeCoupon(id, operator, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return Result.ok(result);
    }

    @PostMapping("/check-expire")
    public Result<Map<String, Object>> checkAndExpireCoupons() {
        couponService.checkAndExpireCoupons();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return Result.ok(result);
    }

    @GetMapping("/records/coupon/{couponId}")
    public Result<List<CouponRecord>> getRecordsByCouponId(@PathVariable Long couponId) {
        List<CouponRecord> list = couponRecordService.getRecordsByCouponId(couponId);
        return Result.ok(list);
    }

    @GetMapping("/records/customer/{customerId}")
    public Result<List<CouponRecord>> getRecordsByCustomerId(@PathVariable Long customerId) {
        List<CouponRecord> list = couponRecordService.getRecordsByCustomerId(customerId);
        return Result.ok(list);
    }

    @GetMapping("/records/order/{orderId}")
    public Result<List<CouponRecord>> getRecordsByOrderId(@PathVariable Long orderId) {
        List<CouponRecord> list = couponRecordService.getRecordsByOrderId(orderId);
        return Result.ok(list);
    }

    @GetMapping("/records/template/{templateId}")
    public Result<List<CouponRecord>> getRecordsByTemplateId(@PathVariable Long templateId) {
        List<CouponRecord> list = couponRecordService.getRecordsByTemplateId(templateId);
        return Result.ok(list);
    }

    @GetMapping("/discount/calculate")
    public Result<Map<String, Object>> calculateDiscount(
            @RequestParam Long couponId,
            @RequestParam BigDecimal orderAmount) {
        Coupon coupon = couponService.getCouponById(couponId);
        Map<String, Object> result = new HashMap<>();
        if (coupon == null) {
            result.put("success", false);
            result.put("message", "优惠券不存在");
            return Result.ok(result);
        }
        boolean available = couponService.checkCouponAvailable(coupon, orderAmount);
        BigDecimal discount = couponService.calculateDiscount(coupon, orderAmount);
        result.put("success", true);
        result.put("available", available);
        result.put("discountAmount", discount);
        result.put("coupon", coupon);
        return Result.ok(result);
    }
}
