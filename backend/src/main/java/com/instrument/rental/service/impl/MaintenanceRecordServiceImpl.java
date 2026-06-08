package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.MaintenanceRecord;
import com.instrument.rental.mapper.MaintenanceRecordMapper;
import com.instrument.rental.service.MaintenanceRecordService;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceRecordServiceImpl extends ServiceImpl<MaintenanceRecordMapper, MaintenanceRecord> implements MaintenanceRecordService {
}
