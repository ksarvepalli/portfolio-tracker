package com.portfolio.tracker.controller;

import com.portfolio.tracker.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> getPrice(
            @RequestParam String symbol,
            @RequestParam String type) {

        BigDecimal price = switch (type.toUpperCase()) {
            case "STOCK" -> marketDataService.getStockPrice(symbol);
            case "MUTUAL_FUND" -> marketDataService.getMutualFundNav(symbol);
            case "GOLD" -> marketDataService.getGoldPricePerGram();
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };

        return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "type", type,
                "price", price
        ));
    }
}