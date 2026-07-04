package com.portfolio.tracker.service;

import com.portfolio.tracker.dto.*;
import com.portfolio.tracker.entity.Holding;
import com.portfolio.tracker.entity.Transaction;
import com.portfolio.tracker.entity.User;
import com.portfolio.tracker.exception.ResourceNotFoundException;
import com.portfolio.tracker.repository.HoldingRepository;
import com.portfolio.tracker.repository.TransactionRepository;
import com.portfolio.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final TransactionRepository transactionRepository;
    private final CacheService cacheService;

    public List<HoldingResponse> getUserHoldings(UUID userId) {
        return holdingRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public HoldingResponse addHolding(UUID userId, AddHoldingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Holding holding = new Holding();
        holding.setUser(user);
        holding.setAssetClass(request.getAssetClass());
        holding.setAssetSymbol(request.getAssetSymbol());
        holding.setAssetName(request.getAssetName());
        holding.setQuantity(request.getQuantity());
        holding.setAvgCostPrice(request.getAvgCostPrice());
        holding.setInvestedValue(request.getAvgCostPrice().multiply(request.getQuantity()));
        holding.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        holding.setProperties(request.getProperties());

        Holding saved = holdingRepository.save(holding);

        // Record BUY transaction
        Transaction transaction = new Transaction();
        transaction.setHolding(saved);
        transaction.setTransactionType("BUY");
        transaction.setAssetSymbol(request.getAssetSymbol());
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(request.getAvgCostPrice());
        transaction.setTransactionDate(LocalDate.now());
        transactionRepository.save(transaction);
        log.info("Added holding: {} for user: {}", saved.getId(), userId);
        cacheService.invalidateSummary(userId.toString());  //
        return mapToResponse(saved);
    }

    public void deleteHolding(UUID userId, UUID holdingId) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found"));

        if (!holding.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Holding does not belong to user");
        }

        holdingRepository.delete(holding);
        cacheService.invalidateSummary(userId.toString());  //
        log.info("Deleted holding: {}", holdingId);
    }


    private HoldingResponse mapToResponse(Holding holding) {
        BigDecimal investedValue = holding.getInvestedValue();

        BigDecimal currentPrice = null;
        BigDecimal currentValue = investedValue;  // default
        BigDecimal profitLoss = BigDecimal.ZERO;
        BigDecimal profitLossPercent = null;

        if (isMarketLinked(holding.getAssetClass()) && holding.getAssetSymbol() != null) {
            // STOCK, MUTUAL_FUND, GOLD - fetch live prices
            try {
                currentPrice = marketDataService.getCurrentPrice(
                        holding.getAssetClass(), holding.getAssetSymbol());

                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    currentValue = currentPrice.multiply(holding.getQuantity());
                    profitLoss = currentValue.subtract(investedValue);

                    if (investedValue.compareTo(BigDecimal.ZERO) > 0) {
                        profitLossPercent = profitLoss
                                .divide(investedValue, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch current price for {}: {}",
                        holding.getAssetSymbol(), e.getMessage());
            }
        } else if ("FIXED_DEPOSIT".equals(holding.getAssetClass())) {
            // FD - calculate accrued interest
            currentValue = marketDataService.getFdCurrentValue(investedValue, holding.getProperties());
            profitLoss = currentValue.subtract(investedValue);
            if (investedValue.compareTo(BigDecimal.ZERO) > 0) {
                profitLossPercent = profitLoss
                        .divide(investedValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        } else if ("REAL_ESTATE".equals(holding.getAssetClass())) {
            // Real estate - use current_market_value if set
            if (holding.getCurrentMarketValue() != null
                    && holding.getCurrentMarketValue().compareTo(BigDecimal.ZERO) > 0) {
                currentValue = holding.getCurrentMarketValue();
                profitLoss = currentValue.subtract(investedValue);
                if (investedValue.compareTo(BigDecimal.ZERO) > 0) {
                    profitLossPercent = profitLoss
                            .divide(investedValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                }
            } else {
                currentValue = investedValue;
                profitLoss = BigDecimal.ZERO;
            }
        }

        return HoldingResponse.builder()
                .id(holding.getId())
                .assetClass(holding.getAssetClass())
                .assetSymbol(holding.getAssetSymbol())
                .assetName(holding.getAssetName())
                .quantity(holding.getQuantity())
                .avgCostPrice(holding.getAvgCostPrice())
                .investedValue(investedValue)
                .currentPrice(currentPrice)
                .currentValue(currentValue)
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPercent)
                .currency(holding.getCurrency())
                .properties(holding.getProperties())
                .createdAt(holding.getCreatedAt())
                .build();
    }

    private boolean isMarketLinked(String assetClass) {
        return "STOCK".equals(assetClass) ||
                "MUTUAL_FUND".equals(assetClass) ||
                "GOLD".equals(assetClass);
    }


    public HoldingResponse updateHolding(UUID userId, UUID holdingId, UpdateHoldingRequest request) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found"));

        if (!holding.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Holding does not belong to user");
        }

        if (request.getCurrentMarketValue() != null) {
            holding.setCurrentMarketValue(request.getCurrentMarketValue());
        }
        if (request.getAssetName() != null) {
            holding.setAssetName(request.getAssetName());
        }
        if (request.getProperties() != null) {
            holding.setProperties(request.getProperties());
        }

        holding = holdingRepository.save(holding);
        cacheService.invalidateSummary(userId.toString());  //
        return mapToResponse(holding);
    }
    public List<SellResponse> sellHolding(UUID userId, SellHoldingRequest request) {
        // Find all holdings of this symbol ordered by creation date (FIFO)
        List<Holding> holdings = holdingRepository
                .findByUserIdAndAssetSymbolOrderByCreatedAtAsc(userId, request.getAssetSymbol());

        if (holdings.isEmpty()) {
            throw new ResourceNotFoundException("No holdings found for symbol: " + request.getAssetSymbol());
        }

        BigDecimal remainingToSell = request.getQuantity();
        BigDecimal sellPrice = request.getSellPrice();
        List<SellResponse> sellResults = new ArrayList<>();
        BigDecimal totalAvailable = holdings.stream()
                .map(Holding::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (remainingToSell.compareTo(totalAvailable) > 0) {
            throw new IllegalArgumentException(
                    "Not enough shares. Available: " + totalAvailable + ", Requested: " + remainingToSell);
        }

        for (Holding holding : holdings) {
            if (remainingToSell.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal qtyFromThisLot = holding.getQuantity().min(remainingToSell);
            BigDecimal remainingQty = holding.getQuantity().subtract(qtyFromThisLot);

            // Calculate realized P&L
            BigDecimal costBasis = qtyFromThisLot.multiply(holding.getAvgCostPrice());
            BigDecimal proceeds = qtyFromThisLot.multiply(sellPrice);
            BigDecimal realizedPL = proceeds.subtract(costBasis);

            // 1. Record SELL transaction FIRST
            Transaction transaction = new Transaction();
            transaction.setHolding(holding);
            transaction.setAssetSymbol(request.getAssetSymbol());
            transaction.setTransactionType("SELL");
            transaction.setQuantity(qtyFromThisLot);
            transaction.setPrice(sellPrice);
            transaction.setTransactionDate(request.getSellDate());
            transactionRepository.save(transaction);  // ← Save BEFORE deleting holding

            // 2. Then update or delete holding
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                holding.setQuantity(BigDecimal.ZERO);
                holding.setInvestedValue(BigDecimal.ZERO);
                holdingRepository.save(holding);
            } else {
                holding.setQuantity(remainingQty);
                holding.setInvestedValue(remainingQty.multiply(holding.getAvgCostPrice()));
                holdingRepository.save(holding);
            }

            sellResults.add(SellResponse.builder()
                    .holdingId(holding.getId())
                    .quantitySold(qtyFromThisLot)
                    .buyPrice(holding.getAvgCostPrice())
                    .sellPrice(sellPrice)
                    .realizedPL(realizedPL)
                    .remainingQty(remainingQty.compareTo(BigDecimal.ZERO) > 0 ? remainingQty : BigDecimal.ZERO)
                    .build());

            remainingToSell = remainingToSell.subtract(qtyFromThisLot);
        }
        cacheService.invalidateSummary(userId.toString());  //
        log.info("Sold {} shares of {} at ₹{}", request.getQuantity(), request.getAssetSymbol(), sellPrice);
        return sellResults;
    }
}