package com.example.ecommerce.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private BigDecimal revenueToday;

    private Map<String, Long> orderCountByStatus;

    private List<TopProductDto> top5Products;

    private List<LowStockProductDto> lowStockProducts;

    private boolean partial;

    @Builder.Default
    private List<String> failedParts = new ArrayList<>();

    private long elapsedMs;
}
