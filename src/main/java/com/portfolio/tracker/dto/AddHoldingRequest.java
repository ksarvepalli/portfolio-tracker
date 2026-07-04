package com.portfolio.tracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class AddHoldingRequest {

    @NotBlank(message = "Asset class is required")
    @Pattern(regexp = "STOCK|MUTUAL_FUND|GOLD|REAL_ESTATE|FIXED_DEPOSIT",
            message = "Asset class must be one of: STOCK, MUTUAL_FUND, GOLD, REAL_ESTATE, FIXED_DEPOSIT")
    private String assetClass;

    // Symbol required only for STOCK and MUTUAL_FUND
    private String assetSymbol;

    @NotBlank(message = "Asset name is required")
    @Size(max = 255, message = "Asset name must be under 255 characters")
    private String assetName;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be positive")
    @Digits(integer = 15, fraction = 6, message = "Quantity format invalid")
    private BigDecimal quantity;

    @NotNull(message = "Average cost price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Price format invalid")
    private BigDecimal avgCostPrice;

    @Pattern(regexp = "INR|USD|EUR", message = "Currency must be INR, USD, or EUR")
    private String currency = "INR";
    private BigDecimal currentMarketValue;  // for non-market-linked assets
    private Map<String, Object> properties;
}