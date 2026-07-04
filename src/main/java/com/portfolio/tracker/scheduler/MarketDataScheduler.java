package com.portfolio.tracker.scheduler;

import com.portfolio.tracker.repository.HoldingRepository;
import com.portfolio.tracker.service.CacheService;
import com.portfolio.tracker.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataScheduler {

    private final MarketDataService marketDataService;
    private final HoldingRepository holdingRepository;
    private final CacheService cacheService;

    // Fetch stock prices every 30 minutes during market hours
    // Runs 12 times between 9:30 AM and 4:00 PM IST
    @Scheduled(cron = "0 0/30 9-15 * * MON-FRI", zone = "IST")
    public void refreshStockPrices() {
        if (!marketDataService.isStockMarketOpen()) {
            log.debug("Market closed, skipping stock price refresh");
            return;
        }

        log.info("Refreshing stock prices...");
        List<String> symbols = holdingRepository.findDistinctSymbolsByAssetClass("STOCK");

        for (String symbol : symbols) {
            try {
                marketDataService.getStockPrice(symbol); // Fetches and caches
                Thread.sleep(15000); // ~4 calls/min, well within 5/min limit for 12 stocks
            } catch (Exception e) {
                log.error("Failed to fetch price for {}: {}", symbol, e.getMessage());
            }
        }
        if (!symbols.isEmpty()) {
            cacheService.invalidateAllSummaries();  // ← Invalidate once after all stocks
        }
        log.info("Stock price refresh complete for {} symbols", symbols.size());
    }

    // Fetch MF NAVs and gold once daily at 6 PM
    @Scheduled(cron = "0 0 18 * * *", zone = "IST")
    public void refreshDailyPrices() {
        log.info("Refreshing MF NAVs and gold price...");
        boolean updated = false;
        // Mutual funds
        List<String> mfSchemes = holdingRepository.findDistinctSymbolsByAssetClass("MUTUAL_FUND");
        for (String scheme : mfSchemes) {
            try {
                marketDataService.getMutualFundNav(scheme);
            } catch (Exception e) {
                log.error("Failed to fetch NAV for {}: {}", scheme, e.getMessage());
            }
        }

        // Gold
        try {
            marketDataService.getGoldPricePerGram();
            updated = true;
        } catch (Exception e) {
            log.error("Failed to fetch gold price: {}", e.getMessage());
        }
        if (updated) {
            cacheService.invalidateAllSummaries();  // ← Invalidate once after all daily fetches
        }
        log.info("Daily price refresh complete");
    }
}