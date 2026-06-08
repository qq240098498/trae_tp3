package com.instrument.rental.dto;

import lombok.Data;

@Data
public class DashboardVO {
    private long totalInstruments;
    private long availableInstruments;
    private long rentedInstruments;
    private long totalCustomers;
    private long activeOrders;
    private long overdueOrders;
    private long pendingReminders;
}
