package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, UUID> {

    List<NetWorthSnapshot> findByUserIdOrderBySnapshotDateAsc(UUID userId);

    List<NetWorthSnapshot> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID userId, LocalDate from, LocalDate to);

    Optional<NetWorthSnapshot> findTopByUserIdOrderBySnapshotDateDesc(UUID userId);

    // Used by sync to check if today's snapshot already exists before insert
    Optional<NetWorthSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate date);
}