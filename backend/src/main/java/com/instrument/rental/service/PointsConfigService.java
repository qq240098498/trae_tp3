package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.entity.PointsConfig;

import java.math.BigDecimal;

public interface PointsConfigService extends IService<PointsConfig> {

    PointsConfig getConfigByKey(String configKey);

    BigDecimal getEarnRate();

    BigDecimal getDeductRate();

    BigDecimal getMaxDeductPercent();

    PointsConfig updateConfig(String configKey, BigDecimal configValue, String description);
}
