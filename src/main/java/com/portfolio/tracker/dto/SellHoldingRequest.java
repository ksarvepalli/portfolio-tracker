package com.portfolio.tracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SellHoldingRequest {
    @NotBlank
    private String assetSymbol;

    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal sellPrice;

    @NotNull
    private LocalDate sellDate;
}