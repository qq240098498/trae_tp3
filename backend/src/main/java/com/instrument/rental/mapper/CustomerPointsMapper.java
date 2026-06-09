package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.CustomerPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerPointsMapper extends BaseMapper<CustomerPoints> {

    @Update("""
            UPDATE customer_points
            SET available_points = available_points + #{pointsDelta},
                total_points = total_points + #{totalDelta},
                used_points = used_points + #{usedDelta},
                update_time = NOW()
            WHERE customer_id = #{customerId}
              AND available_points + #{pointsDelta} >= 0
              AND deleted = 0
            """)
    int updatePointsWithBalanceCheck(@Param("customerId") Long customerId,
                                     @Param("pointsDelta") Integer pointsDelta,
                                     @Param("totalDelta") Integer totalDelta,
                                     @Param("usedDelta") Integer usedDelta);
}
