package com.portfolio.tracker.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthSnapshotId implements Serializable {
    private UUID userId;
    private LocalDate snapshotDate;
}