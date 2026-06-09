package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.CouponTemplate;

import java.util.List;

public interface CouponTemplateService extends IService<CouponTemplate> {

    CouponTemplate createTemplate(CouponTemplate template);

    CouponTemplate updateTemplate(CouponTemplate template);

    boolean deleteTemplate(Long id);

    CouponTemplate getTemplateById(Long id);

    List<CouponTemplate> getAllTemplates();

    List<CouponTemplate> getActiveTemplates();

    boolean incrementIssuedCount(Long templateId);

    boolean incrementUsedCount(Long templateId);
}
