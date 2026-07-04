package com.portfolio.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "net_worth_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(NetWorthSnapshotId.class)
public class NetWorthSnapshot {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> breakdown;
}