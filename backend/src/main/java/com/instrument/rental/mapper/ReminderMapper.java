package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {
}
