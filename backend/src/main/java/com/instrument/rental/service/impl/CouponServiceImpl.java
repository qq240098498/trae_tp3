package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.Coupon;
import com.instrument.rental.entity.CouponRecord;
import com.instrument.rental.entity.CouponTemplate;
import com.instrument.rental.mapper.CouponMapper;
import com.instrument.rental.service.CouponRecordService;
import com.instrument.rental.service.CouponService;
import com.instrument.rental.service.CouponTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Autowired
    private CouponRecordService couponRecordService;

    @Override
    @Transactional
    public Coupon issueCoupon(Long templateId, Long customerId, String operator, String remark) {
        CouponTemplate template = couponTemplateService.getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("优惠券模板不存在");
        }
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new RuntimeException("优惠券模板未激活");
        }

        boolean updated = couponTemplateService.incrementIssuedCount(templateId);
        if (!updated) {
            throw new RuntimeException("优惠券已发放完毕");
        }

        LocalDate validStartDate;
        LocalDate validEndDate;
        if (template.getValidDays() != null && template.getValidDays() > 0) {
            validStartDate = LocalDate.now();
            validEndDate = validStartDate.plusDays(template.getValidDays());
        } else {
            validStartDate = template.getValidStartDate() != null ? template.getValidStartDate() : LocalDate.now();
            validEndDate = template.getValidEndDate() != null ? template.getValidEndDate() : LocalDate.now().plusDays(30);
        }

        Coupon coupon = new Coupon();
        String couponNo = generateCouponNo();
        coupon.setCouponNo(couponNo);
        coupon.setTemplateId(templateId);
        coupon.setCustomerId(customerId);
        coupon.setType(template.getType());
        coupon.setDiscountValue(template.getDiscountValue());
        coupon.setMinAmount(template.getMinAmount() != null ? template.getMinAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(template.getMaxDiscountAmount());
        coupon.setPointsCompatible(template.getPointsCompatible() != null ? template.getPointsCompatible() : true);
        coupon.setStatus("AVAILABLE");
        coupon.setValidStartDate(validStartDate);
        coupon.setValidEndDate(validEndDate);
        coupon.setRemark(remark);
        this.save(coupon);

        couponRecordService.addRecord(
                coupon.getId(),
                customerId,
                templateId,
                null,
                "ISSUE",
                null,
                operator,
                remark != null ? remark : "发放优惠券"
        );

        return coupon;
    }

    @Override
    @Transactional
    public List<Coupon> issueCouponsBatch(Long templateId, List<Long> customerIds, String operator, String remark) {
        List<Coupon> coupons = new ArrayList<>();
        for (Long customerId : customerIds) {
            Coupon coupon = issueCoupon(templateId, customerId, operator, remark);
            coupons.add(coupon);
        }
        return coupons;
    }

    @Override
    public Coupon getCouponById(Long id) {
        return this.getById(id);
    }

    @Override
    public Coupon getCouponByNo(String couponNo) {
        return this.getOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponNo, couponNo));
    }

    @Override
    public List<Coupon> getCustomerCoupons(Long customerId) {
        return this.list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCustomerId, customerId)
                .orderByDesc(Coupon::getCreateTime));
    }

    @Override
    public List<Coupon> getCustomerAvailableCoupons(Long customerId) {
        checkAndExpireCoupons();
        LocalDate today = LocalDate.now();
        return this.list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCustomerId, customerId)
                .eq(Coupon::getStatus, "AVAILABLE")
                .le(Coupon::getValidStartDate, today)
                .ge(Coupon::getValidEndDate, today)
                .orderByAsc(Coupon::getValidEndDate));
    }

    @Override
    public List<Coupon> getCustomerAvailableCouponsForAmount(Long customerId, BigDecimal amount) {
        List<Coupon> availableCoupons = getCustomerAvailableCoupons(customerId);
        List<Coupon> result = new ArrayList<>();
        for (Coupon coupon : availableCoupons) {
            if (checkCouponAvailable(coupon, amount)) {
                result.add(coupon);
            }
        }
        result.sort((c1, c2) -> {
            BigDecimal d1 = calculateDiscount(c1, amount);
            BigDecimal d2 = calculateDiscount(c2, amount);
            return d2.compareTo(d1);
        });
        return result;
    }

    @Override
    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId, BigDecimal orderAmount) {
        Coupon coupon = this.getById(couponId);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (!"AVAILABLE".equals(coupon.getStatus())) {
            throw new RuntimeException("优惠券状态异常，当前状态：" + coupon.getStatus());
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidStartDate())) {
            throw new RuntimeException("优惠券尚未生效");
        }
        if (today.isAfter(coupon.getValidEndDate())) {
            coupon.setStatus("EXPIRED");
            this.updateById(coupon);
            throw new RuntimeException("优惠券已过期");
        }

        if (!checkCouponAvailable(coupon, orderAmount)) {
            throw new RuntimeException("订单金额不满足优惠券最低使用条件");
        }

        BigDecimal discountAmount = calculateDiscount(coupon, orderAmount);

        coupon.setStatus("USED");
        coupon.setOrderId(orderId);
        coupon.setUsedTime(LocalDateTime.now());
        this.updateById(coupon);

        couponTemplateService.incrementUsedCount(coupon.getTemplateId());

        couponRecordService.addRecord(
                coupon.getId(),
                coupon.getCustomerId(),
                coupon.getTemplateId(),
                orderId,
                "USE",
                discountAmount,
                null,
                "订单使用优惠券，抵扣金额：" + discountAmount
        );

        return coupon;
    }

    @Override
    @Transactional
    public boolean expireCoupon(Long couponId) {
        Coupon coupon = this.getById(couponId);
        if (coupon == null || !"AVAILABLE".equals(coupon.getStatus())) {
            return false;
        }
        coupon.setStatus("EXPIRED");
        this.updateById(coupon);

        couponRecordService.addRecord(
                coupon.getId(),
                coupon.getCustomerId(),
                coupon.getTemplateId(),
                null,
                "EXPIRE",
                null,
                null,
                "优惠券过期"
        );
        return true;
    }

    @Override
    @Transactional
    public boolean revokeCoupon(Long couponId, String operator, String remark) {
        Coupon coupon = this.getById(couponId);
        if (coupon == null || !"AVAILABLE".equals(coupon.getStatus())) {
            return false;
        }
        coupon.setStatus("EXPIRED");
        this.updateById(coupon);

        couponRecordService.addRecord(
                coupon.getId(),
                coupon.getCustomerId(),
                coupon.getTemplateId(),
                null,
                "REVOKE",
                null,
                operator,
                remark != null ? remark : "管理员撤回优惠券"
        );
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        if (coupon == null || orderAmount == null) {
            return BigDecimal.ZERO;
        }
        if (!checkCouponAvailable(coupon, orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if ("FIXED".equals(coupon.getType())) {
            discount = coupon.getDiscountValue();
        } else if ("PERCENT".equals(coupon.getType())) {
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                    coupon.getDiscountValue().divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP)
            )).setScale(2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        return discount;
    }

    @Override
    public boolean checkCouponAvailable(Coupon coupon, BigDecimal orderAmount) {
        if (coupon == null) {
            return false;
        }
        if (!"AVAILABLE".equals(coupon.getStatus())) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidStartDate()) || today.isAfter(coupon.getValidEndDate())) {
            return false;
        }
        if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public void checkAndExpireCoupons() {
        LocalDate today = LocalDate.now();
        List<Coupon> expiredCoupons = this.list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, "AVAILABLE")
                .lt(Coupon::getValidEndDate, today));
        for (Coupon coupon : expiredCoupons) {
            expireCoupon(coupon.getId());
        }
    }

    private String generateCouponNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CP" + dateStr + uuid;
    }
}
