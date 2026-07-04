package com.portfolio.tracker.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private UUID holdingId;
    private String assetSymbol;
    private String assetName;
    private String transactionType;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDate transactionDate;
    private String notes;
}