package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.*;
import com.portfolio.tracker.service.HoldingService;
import com.portfolio.tracker.service.MarketDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;
    private final MarketDataService marketDataService;

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getHoldings(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(holdingService.getUserHoldings(userId));
    }

    @PostMapping
    public ResponseEntity<HoldingResponse> addHolding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddHoldingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(holdingService.addHolding(userId, request));
    }
    @PutMapping("/{id}")
    public ResponseEntity<HoldingResponse> updateHolding(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHoldingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(holdingService.updateHolding(userId, id, request));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHolding(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        holdingService.deleteHolding(userId, id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/test/price")
    public ResponseEntity<BigDecimal> testPrice(@RequestParam String symbol,@RequestParam String type) {
        return ResponseEntity.ok(marketDataService.getCurrentPrice(type,symbol));
    }

    @PostMapping("/sell")
    public ResponseEntity<List<SellResponse>> sellHolding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellHoldingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(holdingService.sellHolding(userId, request));
    }
}