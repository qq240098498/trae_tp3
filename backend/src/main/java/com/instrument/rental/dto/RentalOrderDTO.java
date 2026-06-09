package com.instrument.rental.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RentalOrderDTO {
    private Long customerId;
    private Long instrumentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String payMethod;
    private String remark;
    private Boolean usePoints;
    private Integer usePointsAmount;
}
