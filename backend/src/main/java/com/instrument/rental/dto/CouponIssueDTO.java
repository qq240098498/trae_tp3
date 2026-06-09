package com.instrument.rental.dto;

import lombok.Data;

@Data
public class CouponIssueDTO {
    private Long templateId;
    private Long customerId;
    private String operator;
    private String remark;
}
