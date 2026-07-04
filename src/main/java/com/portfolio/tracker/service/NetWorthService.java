package com.portfolio.tracker.service;

import com.portfolio.tracker.dto.DailySnapshotResponse;
import com.portfolio.tracker.dto.HoldingResponse;
import com.portfolio.tracker.dto.PortfolioSummaryResponse;
import com.portfolio.tracker.entity.NetWorthSnapshot;
import com.portfolio.tracker.repository.NetWorthSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NetWorthService {

    private final HoldingService holdingService;
    private final NetWorthSnapshotRepository snapshotRepository;

    public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        List<HoldingResponse> holdings = holdingService.getUserHoldings(userId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        Map<String, PortfolioSummaryResponse.AssetAllocation> allocationMap = new LinkedHashMap<>();

        for (HoldingResponse h : holdings) {
            BigDecimal invested = h.getInvestedValue() != null ? h.getInvestedValue() : BigDecimal.ZERO;
            totalInvested = totalInvested.add(invested);

            BigDecimal current = h.getCurrentValue() != null ? h.getCurrentValue() : invested;
            totalCurrentValue = totalCurrentValue.add(current);

            allocationMap.compute(h.getAssetClass(), (key, existing) -> {
                if (existing == null) {
                    return PortfolioSummaryResponse.AssetAllocation.builder()
                            .investedValue(invested)
                            .currentValue(current)
                            .profitLoss(current.subtract(invested))
                            .build();
                } else {
                    existing.setInvestedValue(existing.getInvestedValue().add(invested));
                    existing.setCurrentValue(existing.getCurrentValue().add(current));
                    existing.setProfitLoss(existing.getCurrentValue().subtract(existing.getInvestedValue()));
                    return existing;
                }
            });
        }

        // Calculate percentages
        for (PortfolioSummaryResponse.AssetAllocation alloc : allocationMap.values()) {
            if (totalCurrentValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = alloc.getCurrentValue()
                        .divide(totalCurrentValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                alloc.setPercentage(pct);
            } else {
                alloc.setPercentage(BigDecimal.ZERO);
            }
        }

        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalProfitLossPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossPercent = totalProfitLoss
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Day change
        BigDecimal yesterdayValue = getYesterdaySnapshot(userId);
        BigDecimal dayChange = totalCurrentValue.subtract(yesterdayValue);
        BigDecimal dayChangePercent = BigDecimal.ZERO;
        if (yesterdayValue.compareTo(BigDecimal.ZERO) > 0) {
            dayChangePercent = dayChange
                    .divide(yesterdayValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return PortfolioSummaryResponse.builder()
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .dayChange(dayChange)
                .dayChangePercent(dayChangePercent)
                .allocation(allocationMap)
                .totalHoldings(holdings.size())
                .build();
    }

    public void saveDailySnapshot(UUID userId) {
        PortfolioSummaryResponse summary = getPortfolioSummary(userId);

        Map<String, Object> breakdown = new HashMap<>();
        summary.getAllocation().forEach((key, alloc) ->
                breakdown.put(key, alloc.getCurrentValue()));

        NetWorthSnapshot snapshot = snapshotRepository
                .findByUserIdAndSnapshotDate(userId, LocalDate.now())
                .orElse(NetWorthSnapshot.builder()
                        .userId(userId)
                        .snapshotDate(LocalDate.now())
                        .build());

        snapshot.setTotalValue(summary.getTotalCurrentValue());
        snapshot.setBreakdown(breakdown);
        snapshotRepository.save(snapshot);

        log.info("Saved daily snapshot for user {}: ₹{}", userId, summary.getTotalCurrentValue());
    }

    public List<DailySnapshotResponse> getNetWorthHistory(UUID userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);

        return snapshotRepository
                .findByUserIdAndSnapshotDateAfterOrderBySnapshotDateAsc(userId, startDate)
                .stream()
                .map(s -> DailySnapshotResponse.builder()
                        .date(s.getSnapshotDate())
                        .totalValue(s.getTotalValue())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal getYesterdaySnapshot(UUID userId) {
        return snapshotRepository
                .findByUserIdAndSnapshotDate(userId, LocalDate.now().minusDays(1))
                .map(NetWorthSnapshot::getTotalValue)
                .orElse(BigDecimal.ZERO);
    }
}