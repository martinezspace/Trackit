package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.SyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {

    // Paginated - connections accumulate many sync logs over time
    Page<SyncLog> findByConnectionIdOrderByStartedAtDesc(UUID connectionId, Pageable pageable);
}