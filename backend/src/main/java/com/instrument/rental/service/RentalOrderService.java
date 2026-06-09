package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.dto.PointsCalculateVO;
import com.instrument.rental.dto.RenewalDTO;
import com.instrument.rental.dto.RentalOrderDTO;
import com.instrument.rental.dto.ReturnDTO;
import com.instrument.rental.entity.RentalOrder;

import java.time.LocalDate;

public interface RentalOrderService extends IService<RentalOrder> {

    RentalOrder createOrder(RentalOrderDTO dto);

    RentalOrder renewOrder(RenewalDTO dto);

    RentalOrder returnOrder(ReturnDTO dto);

    PointsCalculateVO calculatePoints(Long customerId, Long instrumentId, LocalDate startDate, LocalDate endDate, Integer usePoints);
}
