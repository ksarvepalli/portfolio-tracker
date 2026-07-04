package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "holdings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "asset_class", nullable = false)
    private String assetClass;

    @Column(name = "asset_symbol")
    private String assetSymbol;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal quantity;

    @Column(name = "avg_cost_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal avgCostPrice;
    @Column(name = "invested_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal investedValue;
    @Column(name = "current_market_value", precision = 15, scale = 4)
    private BigDecimal currentMarketValue;

    @Column(length = 3)
    private String currency = "INR";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getCurrentMarketValue() {
        return currentMarketValue;
    }
}