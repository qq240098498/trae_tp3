package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.RepairOrder;
import com.instrument.rental.mapper.RepairOrderMapper;
import com.instrument.rental.service.RepairOrderService;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {
}
