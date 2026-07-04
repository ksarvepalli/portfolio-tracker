package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    @Query("SELECT h FROM Holding h WHERE h.user.id = :userId AND h.quantity > 0")
    List<Holding> findByUserId(UUID userId);
    @Query("SELECT DISTINCT h.assetSymbol FROM Holding h WHERE h.assetClass = :assetClass AND h.assetSymbol IS NOT NULL")
    List<String> findDistinctSymbolsByAssetClass(@Param("assetClass") String assetClass);
    List<Holding> findByUserIdAndAssetSymbolOrderByCreatedAtAsc(UUID userId, String assetSymbol);

}