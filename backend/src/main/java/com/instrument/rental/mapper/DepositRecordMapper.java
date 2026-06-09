package com.instrument.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.instrument.rental.entity.DepositRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface DepositRecordMapper extends BaseMapper<DepositRecord> {

    @Insert("""
            INSERT INTO deposit_record (order_id, type, amount, pay_method, status, remark)
            SELECT #{orderId}, 'REFUND', #{amount}, #{payMethod}, 'PAID', #{remark}
            FROM DUAL
            WHERE (
                SELECT COALESCE(SUM(CASE
                    WHEN type = 'COLLECT' THEN amount
                    WHEN type IN ('REFUND', 'DEDUCT') THEN -amount
                    ELSE 0
                END), 0)
                FROM deposit_record
                WHERE order_id = #{orderId}
                  AND status IN ('PAID', 'COMPLETED')
                  AND deleted = 0
            ) >= #{amount}
            """)
    int insertRefundWithBalanceCheck(@Param("orderId") Long orderId,
                                     @Param("amount") BigDecimal amount,
                                     @Param("payMethod") String payMethod,
                                     @Param("remark") String remark);

    @Insert("""
            INSERT INTO deposit_record (order_id, type, amount, pay_method, status, remark)
            SELECT #{orderId}, 'DEDUCT', #{amount}, NULL, 'PAID', #{remark}
            FROM DUAL
            WHERE (
                SELECT COALESCE(SUM(CASE
                    WHEN type = 'COLLECT' THEN amount
                    WHEN type IN ('REFUND', 'DEDUCT') THEN -amount
                    ELSE 0
                END), 0)
                FROM deposit_record
                WHERE order_id = #{orderId}
                  AND status IN ('PAID', 'COMPLETED')
                  AND deleted = 0
            ) >= #{amount}
            """)
    int insertDeductWithBalanceCheck(@Param("orderId") Long orderId,
                                     @Param("amount") BigDecimal amount,
                                     @Param("remark") String remark);
}
