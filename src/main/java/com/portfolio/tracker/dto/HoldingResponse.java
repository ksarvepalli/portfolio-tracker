package com.portfolio.tracker.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {
    private UUID id;
    private String assetClass;
    private String assetSymbol;
    private String assetName;
    private BigDecimal quantity;
    private BigDecimal avgCostPrice;
    private BigDecimal investedValue;      // quantity * avgCostPrice
    private BigDecimal currentPrice;       // live price from API
    private BigDecimal currentValue;       // quantity * currentPrice
    private BigDecimal profitLoss;         // currentValue - investedValue
    private BigDecimal profitLossPercent;  // ((currentValue - investedValue) / investedValue) * 100
    private String currency;
    private Map<String, Object> properties;
    private LocalDateTime createdAt;
}