package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.DamageRegistration;
import com.instrument.rental.mapper.DamageRegistrationMapper;
import com.instrument.rental.service.DamageRegistrationService;
import org.springframework.stereotype.Service;

@Service
public class DamageRegistrationServiceImpl extends ServiceImpl<DamageRegistrationMapper, DamageRegistration> implements DamageRegistrationService {
}
