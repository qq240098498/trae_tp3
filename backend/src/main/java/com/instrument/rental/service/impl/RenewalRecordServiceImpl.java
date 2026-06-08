package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.RenewalRecord;
import com.instrument.rental.mapper.RenewalRecordMapper;
import com.instrument.rental.service.RenewalRecordService;
import org.springframework.stereotype.Service;

@Service
public class RenewalRecordServiceImpl extends ServiceImpl<RenewalRecordMapper, RenewalRecord> implements RenewalRecordService {
}
