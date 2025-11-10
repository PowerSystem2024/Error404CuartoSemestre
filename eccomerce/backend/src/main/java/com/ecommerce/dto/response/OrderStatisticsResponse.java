package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByStatus;
    private Long ordersToday;
    private Long ordersThisWeek;
    private Long ordersThisMonth;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
    private Double averageOrderValue;
}