package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.Instrument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InstrumentMapper extends BaseMapper<Instrument> {
}
