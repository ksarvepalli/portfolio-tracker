package com.portfolio.tracker.dto;

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
public class PortfolioSummaryResponse {
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private BigDecimal dayChange;           // change since yesterday
    private BigDecimal dayChangePercent;
    private Map<String, AssetAllocation> allocation;  // STOCK -> {value, percent}
    private int totalHoldings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAllocation {
        private BigDecimal investedValue;
        private BigDecimal currentValue;
        private BigDecimal profitLoss;
        private BigDecimal percentage;       // % of total portfolio
    }
}