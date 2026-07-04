package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.TransactionResponse;
import com.portfolio.tracker.entity.Holding;
import com.portfolio.tracker.entity.Transaction;
import com.portfolio.tracker.exception.ResourceNotFoundException;
import com.portfolio.tracker.repository.HoldingRepository;
import com.portfolio.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;

    // Get all transactions for a specific holding
    @GetMapping("/holding/{holdingId}")
    public ResponseEntity<List<TransactionResponse>> getByHolding(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID holdingId) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found"));

        if (!holding.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Holding does not belong to user");
        }

        return ResponseEntity.ok(mapToResponse(
                transactionRepository.findByHoldingIdOrderByTransactionDateDesc(holdingId)));
    }

    // Get all transactions for the logged-in user (all holdings)
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        List<Transaction> transactions = transactionRepository
                .findByHoldingUserIdOrderByTransactionDateDesc(userId);

        return ResponseEntity.ok(mapToResponse(transactions));
    }

    private List<TransactionResponse> mapToResponse(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .holdingId(t.getHolding().getId())
                        .assetSymbol(t.getHolding().getAssetSymbol())
                        .assetName(t.getHolding().getAssetName())
                        .transactionType(t.getTransactionType())
                        .quantity(t.getQuantity())
                        .price(t.getPrice())
                        .transactionDate(t.getTransactionDate())
                        .notes(t.getNotes())
                        .build())
                .toList();
    }
}