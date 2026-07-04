package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.PortfolioSummaryResponse;
import com.portfolio.tracker.dto.DailySnapshotResponse;
import com.portfolio.tracker.service.CacheService;
import com.portfolio.tracker.service.NetWorthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final NetWorthService netWorthService;
    private final CacheService cacheService;  // Add this

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String userIdStr = userId.toString();

        // Check cache first
        Optional<Object> cached = cacheService.getSummary(userIdStr);
        if (cached.isPresent() && cached.get() instanceof PortfolioSummaryResponse) {
            log.debug("Returning cached summary for user: {}", userIdStr);
            return ResponseEntity.ok((PortfolioSummaryResponse) cached.get());
        }

        // Compute and cache
        PortfolioSummaryResponse summary = netWorthService.getPortfolioSummary(userId);
        cacheService.cacheSummary(userIdStr, summary, Duration.ofMinutes(5));
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DailySnapshotResponse>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") int days) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(netWorthService.getNetWorthHistory(userId, days));
    }
}