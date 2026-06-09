package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.WorkLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLog> {
}
