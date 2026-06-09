package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("UPDATE coupon_template SET issued_count = issued_count + 1 WHERE id = #{id} AND (total_count = -1 OR issued_count < total_count) AND deleted = 0")
    int incrementIssuedCount(@Param("id") Long id);

    @Update("UPDATE coupon_template SET used_count = used_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementUsedCount(@Param("id") Long id);
}
