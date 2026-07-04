package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByHoldingIdOrderByTransactionDateDesc(UUID holdingId);
    List<Transaction> findByHoldingUserIdOrderByTransactionDateDesc(UUID userId);
}