package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.DepositRecord;
import com.instrument.rental.mapper.DepositRecordMapper;
import com.instrument.rental.service.DepositRecordService;
import org.springframework.stereotype.Service;

@Service
public class DepositRecordServiceImpl extends ServiceImpl<DepositRecordMapper, DepositRecord> implements DepositRecordService {
}
