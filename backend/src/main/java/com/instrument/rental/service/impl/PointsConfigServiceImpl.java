package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.PointsConfig;
import com.instrument.rental.mapper.PointsConfigMapper;
import com.instrument.rental.service.PointsConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PointsConfigServiceImpl extends ServiceImpl<PointsConfigMapper, PointsConfig> implements PointsConfigService {

    public static final String KEY_EARN_RATE = "EARN_RATE";
    public static final String KEY_DEDUCT_RATE = "DEDUCT_RATE";
    public static final String KEY_MAX_DEDUCT_PERCENT = "MAX_DEDUCT_PERCENT";

    @Override
    public PointsConfig getConfigByKey(String configKey) {
        return this.getOne(new LambdaQueryWrapper<PointsConfig>().eq(PointsConfig::getConfigKey, configKey));
    }

    @Override
    public BigDecimal getEarnRate() {
        PointsConfig config = getConfigByKey(KEY_EARN_RATE);
        return config != null ? config.getConfigValue() : BigDecimal.ONE;
    }

    @Override
    public BigDecimal getDeductRate() {
        PointsConfig config = getConfigByKey(KEY_DEDUCT_RATE);
        return config != null ? config.getConfigValue() : BigDecimal.valueOf(100);
    }

    @Override
    public BigDecimal getMaxDeductPercent() {
        PointsConfig config = getConfigByKey(KEY_MAX_DEDUCT_PERCENT);
        return config != null ? config.getConfigValue() : BigDecimal.valueOf(30);
    }

    @Override
    public PointsConfig updateConfig(String configKey, BigDecimal configValue, String description) {
        PointsConfig config = getConfigByKey(configKey);
        if (config == null) {
            config = new PointsConfig();
            config.setConfigKey(configKey);
            config.setConfigName(getDefaultConfigName(configKey));
        }
        config.setConfigValue(configValue);
        if (description != null) {
            config.setDescription(description);
        }
        this.saveOrUpdate(config);
        return config;
    }

    private String getDefaultConfigName(String configKey) {
        switch (configKey) {
            case KEY_EARN_RATE:
                return "消费积分获得比例";
            case KEY_DEDUCT_RATE:
                return "积分抵扣现金比例";
            case KEY_MAX_DEDUCT_PERCENT:
                return "积分抵扣最高百分比";
            default:
                return configKey;
        }
    }
}
