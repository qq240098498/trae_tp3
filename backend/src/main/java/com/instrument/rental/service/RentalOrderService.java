package com.instrument.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.instrument.rental.dto.RenewalDTO;
import com.instrument.rental.dto.RentalOrderDTO;
import com.instrument.rental.dto.ReturnDTO;
import com.instrument.rental.entity.RentalOrder;

public interface RentalOrderService extends IService<RentalOrder> {

    RentalOrder createOrder(RentalOrderDTO dto);

    RentalOrder renewOrder(RenewalDTO dto);

    RentalOrder returnOrder(ReturnDTO dto);
}
