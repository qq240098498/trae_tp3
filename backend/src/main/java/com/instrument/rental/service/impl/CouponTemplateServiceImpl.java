package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.CouponTemplate;
import com.instrument.rental.mapper.CouponTemplateMapper;
import com.instrument.rental.service.CouponTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Override
    public CouponTemplate createTemplate(CouponTemplate template) {
        if (template.getStatus() == null) {
            template.setStatus("ACTIVE");
        }
        if (template.getTotalCount() == null) {
            template.setTotalCount(-1);
        }
        if (template.getUsedCount() == null) {
            template.setUsedCount(0);
        }
        if (template.getIssuedCount() == null) {
            template.setIssuedCount(0);
        }
        if (template.getPointsCompatible() == null) {
            template.setPointsCompatible(true);
        }
        this.save(template);
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(CouponTemplate template) {
        this.updateById(template);
        return this.getById(template.getId());
    }

    @Override
    public boolean deleteTemplate(Long id) {
        return this.removeById(id);
    }

    @Override
    public CouponTemplate getTemplateById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<CouponTemplate> getAllTemplates() {
        return this.list(new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getCreateTime));
    }

    @Override
    public List<CouponTemplate> getActiveTemplates() {
        return this.list(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, "ACTIVE")
                .orderByDesc(CouponTemplate::getCreateTime));
    }

    @Override
    public boolean incrementIssuedCount(Long templateId) {
        int rows = couponTemplateMapper.incrementIssuedCount(templateId);
        return rows > 0;
    }

    @Override
    public boolean incrementUsedCount(Long templateId) {
        int rows = couponTemplateMapper.incrementUsedCount(templateId);
        return rows > 0;
    }
}
