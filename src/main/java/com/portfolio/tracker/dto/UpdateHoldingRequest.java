package com.portfolio.tracker.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateHoldingRequest {
    private BigDecimal currentMarketValue;  // update market value
    private String assetName;
    private java.util.Map<String, Object> properties;
}