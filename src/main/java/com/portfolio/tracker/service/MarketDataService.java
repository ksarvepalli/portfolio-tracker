package com.portfolio.tracker.service;


import com.portfolio.tracker.config.MarketApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final WebClient webClient;
    private final CacheService cacheService;
    @Value("${app.market.yahoo-finance.base-url}")
    private String stockurl;
    @Value("${app.market.mfapi.base-url}")
    private String mfUrl;
    @Value("${app.market.gold-api.base-url}")
    private String goldUrl;
    @Value("${app.market.gold-api.apikey}")
    private String apikey;

    private static final Duration STOCK_CACHE_TTL = Duration.ofMinutes(35);     // Slightly more than fetch interval
    private static final Duration MF_CACHE_TTL = Duration.ofHours(25);          // More than 24 hours
    private static final Duration GOLD_CACHE_TTL = Duration.ofHours(25);

    // ============ STOCK PRICE ============

    public BigDecimal getStockPrice(String symbol) {
        // Try cache first
        return cacheService.getPrice("STOCK", symbol)
                .orElseGet(() -> fetchAndCacheStockPrice(symbol));
    }

    private BigDecimal fetchAndCacheStockPrice(String symbol) {
        try {
            // Try NSE first, fall back to BSE
            BigDecimal price = fetchFromYahoo(symbol + ".NS");
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                cacheService.cachePrice("STOCK", symbol, price, STOCK_CACHE_TTL);
                log.info("Fetched stock {} (NSE): ₹{}", symbol, price);
                return price;
            }

            price = fetchFromYahoo(symbol + ".BO");
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                cacheService.cachePrice("STOCK", symbol, price, STOCK_CACHE_TTL);
                log.info("Fetched stock {} (BSE): ₹{}", symbol, price);
                return price;
            }

            log.warn("Could not fetch price for {}", symbol);
        } catch (Exception e) {
            log.error("Error fetching stock {}: {}", symbol, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal fetchFromYahoo(String yahooSymbol) {
        try {
            String url = stockurl + yahooSymbol;

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("chart")) {
                Map<String, Object> chart = (Map<String, Object>) response.get("chart");
                List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");

                if (result != null && !result.isEmpty()) {
                    Map<String, Object> meta = (Map<String, Object>) result.get(0).get("meta");
                    double price = ((Number) meta.get("regularMarketPrice")).doubleValue();
                    return new BigDecimal(String.format("%.2f", price));
                }
            }
        } catch (Exception e) {
            log.debug("Yahoo fetch failed for {}: {}", yahooSymbol, e.getMessage());
        }
        return null;
    }

    // ============ MUTUAL FUND NAV ============

    public BigDecimal getMutualFundNav(String schemeCode) {
        return cacheService.getPrice("MUTUAL_FUND", schemeCode)
                .orElseGet(() -> fetchAndCacheNav(schemeCode));
    }

    private BigDecimal fetchAndCacheNav(String schemeCode) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String url = mfUrl + "/" + schemeCode;
                Map<String, Object> response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(15));

                if (response != null && response.containsKey("data")) {
                    List<Map<String, String>> data = (List<Map<String, String>>) response.get("data");
                    if (!data.isEmpty()) {
                        String navStr = data.get(0).get("nav");
                        BigDecimal nav = new BigDecimal(navStr);
                        cacheService.cachePrice("MUTUAL_FUND", schemeCode, nav, MF_CACHE_TTL);
                        log.info("Fetched NAV {}: ₹{}", schemeCode, nav);
                        return nav;
                    }
                }
            } catch (Exception e) {
                log.warn("Attempt {} failed for NAV {}: {}", attempt, schemeCode, e.getMessage());
                if (attempt == 1) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return BigDecimal.ZERO;
    }

    // ============ GOLD PRICE ============

    public BigDecimal getGoldPricePerGram() {
        return cacheService.getPrice("GOLD", "INR")
                .orElseGet(this::fetchAndCacheGoldPrice);
    }

    private BigDecimal fetchAndCacheGoldPrice() {
        try {

            Map<String, Object> response = webClient.get()
                    .uri(goldUrl)
                    .header("x-access-token", apikey)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("price_gram_24k")) {
                double spotPricePerGram = ((Number) response.get("price_gram_24k")).doubleValue();

                // Apply India adjustments:
                // +10% basic customs duty
                // +3% GST
                double indiaPricePerGram = spotPricePerGram * 1.10 * 1.03;

                BigDecimal goldPrice = new BigDecimal(String.format("%.2f", indiaPricePerGram));
                cacheService.cachePrice("GOLD", "INR", goldPrice, GOLD_CACHE_TTL);
                log.info("Fetched gold: spot ₹{}/g → India ₹{}/g (24K)",
                        String.format("%.2f", spotPricePerGram), goldPrice);
                return goldPrice;
            }

            log.warn("Gold API response missing price_gram_24k");

        } catch (Exception e) {
            log.error("Error fetching gold price: {}", e.getMessage());
        }
        return new BigDecimal("7500.00");
    }
    // ============ FD CALCULATION ============

    public BigDecimal getFdCurrentValue(BigDecimal principal, Map<String, Object> properties) {
        if (properties == null || !properties.containsKey("interestRate")) {
            return principal;
        }
        try {
            double rate = Double.parseDouble(properties.get("interestRate").toString().replace("%", ""));
            LocalDate startDate = properties.containsKey("startDate")
                    ? LocalDate.parse(properties.get("startDate").toString())
                    : LocalDate.now();
            long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
            double yearsElapsed = daysElapsed / 365.0;
            double amount = principal.doubleValue() * Math.pow(1 + (rate / 100), yearsElapsed);
            return new BigDecimal(String.format("%.2f", amount));
        } catch (Exception e) {
            log.warn("Error calculating FD value: {}", e.getMessage());
            return principal;
        }
    }

    // ============ GENERIC ============

    public BigDecimal getCurrentPrice(String assetClass, String symbol) {
        return switch (assetClass) {
            case "STOCK" -> getStockPrice(symbol);
            case "MUTUAL_FUND" -> getMutualFundNav(symbol);
            case "GOLD" -> getGoldPricePerGram();
            case "REAL_ESTATE", "FIXED_DEPOSIT" -> null;
            default -> throw new IllegalArgumentException("Unknown asset class: " + assetClass);
        };
    }

    // ============ MARKET HOURS ============

    public boolean isStockMarketOpen() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        return !now.isBefore(LocalTime.of(9, 15)) && !now.isAfter(LocalTime.of(15, 30));
    }

    public boolean isWeekend() {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}