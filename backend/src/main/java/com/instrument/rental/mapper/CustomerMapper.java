package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
