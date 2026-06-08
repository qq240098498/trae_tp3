package com.instrument.rental.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RenewalDTO {
    private Long orderId;
    private LocalDate newEndDate;
    private String remark;
}
