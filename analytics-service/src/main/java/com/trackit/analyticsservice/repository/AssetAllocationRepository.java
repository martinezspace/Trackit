package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.AssetAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetAllocationRepository extends JpaRepository<AssetAllocation, UUID> {

    List<AssetAllocation> findByUserIdOrderByWeightPctDesc(UUID userId);

    // Used by sync to replace all rows for a user — asset allocation is current state, not historical
    void deleteAllByUserId(UUID userId);
}