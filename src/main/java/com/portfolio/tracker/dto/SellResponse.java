package com.portfolio.tracker.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SellResponse {
    private UUID holdingId;
    private BigDecimal quantitySold;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal realizedPL;
    private BigDecimal remainingQty;
}