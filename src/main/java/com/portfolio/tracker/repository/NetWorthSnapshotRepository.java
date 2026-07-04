package com.portfolio.tracker.repository;

import com.portfolio.tracker.entity.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, UUID> {

    List<NetWorthSnapshot> findByUserIdAndSnapshotDateAfterOrderBySnapshotDateAsc(
            UUID userId, LocalDate date);

    Optional<NetWorthSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate date);
}