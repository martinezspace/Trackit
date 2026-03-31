package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    //All snapshots for an account ordered oldest to newest - full portfolio chart
    List<PortfolioSnapshot> findByAccount_IdOrderBySnapshotDateAsc(UUID accountId);

    //Snapshots for a date range - used for 1M, 3M, 1Y chart views
    List<PortfolioSnapshot> findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            UUID accountId, LocalDate from, LocalDate to);

    //Latest snapshot - current portfolio summary card
    Optional<PortfolioSnapshot> findTopByAccount_IdOrderBySnapshotDateDesc(UUID accountId);

    //Duplicate prevention, PriceWorker checks before creating today's snapshot
    boolean existsByAccount_IdAndSnapshotDate(UUID accountId, LocalDate snapshotDate);
}
