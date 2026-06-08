package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.mapper.InstrumentMapper;
import com.instrument.rental.service.InstrumentService;
import org.springframework.stereotype.Service;

@Service
public class InstrumentServiceImpl extends ServiceImpl<InstrumentMapper, Instrument> implements InstrumentService {
}
