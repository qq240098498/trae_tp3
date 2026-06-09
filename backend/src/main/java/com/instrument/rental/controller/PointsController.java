package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.instrument.rental.common.Result;
import com.instrument.rental.dto.PointsCalculateVO;
import com.instrument.rental.entity.CustomerPoints;
import com.instrument.rental.entity.PointsConfig;
import com.instrument.rental.entity.PointsRecord;
import com.instrument.rental.service.CustomerPointsService;
import com.instrument.rental.service.PointsConfigService;
import com.instrument.rental.service.PointsRecordService;
import com.instrument.rental.service.RentalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired
    private PointsConfigService pointsConfigService;

    @Autowired
    private CustomerPointsService customerPointsService;

    @Autowired
    private PointsRecordService pointsRecordService;

    @Autowired
    private RentalOrderService rentalOrderService;

    @GetMapping("/config")
    public Result<List<PointsConfig>> getAllConfig() {
        List<PointsConfig> list = pointsConfigService.list();
        return Result.ok(list);
    }

    @PostMapping("/config")
    public Result<PointsConfig> updateConfig(@RequestBody Map<String, Object> params) {
        String configKey = (String) params.get("configKey");
        BigDecimal configValue = new BigDecimal(params.get("configValue").toString());
        String description = (String) params.get("description");
        PointsConfig config = pointsConfigService.updateConfig(configKey, configValue, description);
        return Result.ok(config);
    }

    @GetMapping("/config/{key}")
    public Result<PointsConfig> getConfig(@PathVariable String key) {
        PointsConfig config = pointsConfigService.getConfigByKey(key);
        return Result.ok(config);
    }

    @GetMapping("/customer/{customerId}")
    public Result<CustomerPoints> getCustomerPoints(@PathVariable Long customerId) {
        CustomerPoints points = customerPointsService.getByCustomerId(customerId);
        return Result.ok(points);
    }

    @GetMapping("/customer/{customerId}/records")
    public Result<List<PointsRecord>> getCustomerPointsRecords(@PathVariable Long customerId) {
        List<PointsRecord> records = pointsRecordService.getRecordsByCustomerId(customerId);
        return Result.ok(records);
    }

    @GetMapping("/calculate")
    public Result<PointsCalculateVO> calculatePoints(
            @RequestParam Long customerId,
            @RequestParam Long instrumentId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Integer usePoints) {
        PointsCalculateVO vo = rentalOrderService.calculatePoints(customerId, instrumentId, startDate, endDate, usePoints);
        return Result.ok(vo);
    }

    @PostMapping("/adjust")
    public Result<Map<String, Object>> adjustPoints(@RequestBody Map<String, Object> params) {
        Long customerId = Long.valueOf(params.get("customerId").toString());
        Integer points = Integer.valueOf(params.get("points").toString());
        String remark = (String) params.get("remark");
        String operator = (String) params.get("operator");
        String type = points > 0 ? "MANUAL_ADD" : "MANUAL_DEDUCT";
        BigDecimal amount = BigDecimal.ZERO;

        boolean success;
        if (points > 0) {
            success = customerPointsService.addPoints(customerId, null, points, amount, operator, remark != null ? remark : "管理员手动增加积分");
        } else {
            success = customerPointsService.deductPoints(customerId, null, Math.abs(points), amount, operator, remark != null ? remark : "管理员手动扣减积分");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("customerPoints", customerPointsService.getByCustomerId(customerId));
        return Result.ok(result);
    }

    @GetMapping("/config/defaults")
    public Result<Map<String, Object>> getDefaultConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("earnRate", pointsConfigService.getEarnRate());
        configs.put("deductRate", pointsConfigService.getDeductRate());
        configs.put("maxDeductPercent", pointsConfigService.getMaxDeductPercent());
        return Result.ok(configs);
    }
}
