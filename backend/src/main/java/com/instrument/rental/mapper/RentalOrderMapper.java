package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.RentalOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RentalOrderMapper extends BaseMapper<RentalOrder> {
}
