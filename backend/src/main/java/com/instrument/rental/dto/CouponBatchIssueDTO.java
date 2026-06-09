package com.instrument.rental.dto;

import lombok.Data;
import java.util.List;

@Data
public class CouponBatchIssueDTO {
    private Long templateId;
    private List<Long> customerIds;
    private String operator;
    private String remark;
}
