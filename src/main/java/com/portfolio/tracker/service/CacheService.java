package com.portfolio.tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRICE_PREFIX = "price:";
    private static final String SUMMARY_PREFIX = "summary:";

    // ---- Price Cache ----

    public void cachePrice(String assetClass, String symbol, BigDecimal price, Duration ttl) {
        String key = PRICE_PREFIX + assetClass + ":" + symbol;
        redisTemplate.opsForValue().set(key, price, ttl);
        log.debug("Cached price: {} = {}", key, price);
    }

    public Optional<BigDecimal> getPrice(String assetClass, String symbol) {
        String key = PRICE_PREFIX + assetClass + ":" + symbol;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof BigDecimal) {
            return Optional.of((BigDecimal) value);
        } else if (value instanceof Number) {
            return Optional.of(new BigDecimal(value.toString()));
        }
        return Optional.empty();
    }

    // ---- Portfolio Summary Cache ----

    public void cacheSummary(String userId, Object summary, Duration ttl) {
        String key = SUMMARY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, summary, ttl);
        log.debug("Cached summary for user: {}", userId);
    }

    public Optional<Object> getSummary(String userId) {
        String key = SUMMARY_PREFIX + userId;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void invalidateSummary(String userId) {
        String key = SUMMARY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Invalidated summary cache for user: {}", userId);
    }

    public void invalidateAllSummaries() {
        redisTemplate.delete(redisTemplate.keys(SUMMARY_PREFIX + "*"));
        log.info("Invalidated all summary caches");
    }
}