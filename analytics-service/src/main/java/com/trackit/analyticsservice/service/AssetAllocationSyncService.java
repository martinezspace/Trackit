package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.InvestmentServiceClient;
import com.trackit.analyticsservice.dto.client.investment.HoldingDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.response.AssetAllocationResponseDTO;
import com.trackit.analyticsservice.mapper.AssetAllocationMapper;
import com.trackit.analyticsservice.model.AssetAllocation;
import com.trackit.analyticsservice.repository.AssetAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAllocationSyncService {

    private final AssetAllocationRepository repository;
    private final AssetAllocationMapper mapper;
    private final InvestmentServiceClient investmentClient;

    // --- Sync ---

    @Transactional
    public void sync(UUID userId) {
        log.info("Starting asset allocation sync for user {}", userId);

        List<InvestmentAccountDTO> accounts = investmentClient.getInvestmentAccounts(userId);
        List<AssetAllocation> allocations = new ArrayList<>();

        for (InvestmentAccountDTO account : accounts) {
            if (!account.isActive()) continue;

            UUID accountId = UUID.fromString(account.getId());
            List<HoldingDTO> holdings = investmentClient.getHoldings(accountId, userId);

            BigDecimal accountValue = holdings.stream()
                    .filter(h -> h.getCurrentValue() != null)
                    .map(h -> new BigDecimal(h.getCurrentValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AssetAllocation allocation = new AssetAllocation();
            allocation.setUserId(userId);
            allocation.setAccountId(accountId);
            allocation.setAccountDisplayName(account.getDisplayName());
            allocation.setAccountType(account.getAccountType());
            // Mirrors account_type until HoldingDTO exposes instrument_type
            allocation.setInstrumentType(account.getAccountType());
            allocation.setCurrentValue(accountValue);
            allocation.setCurrency(account.getCurrency() != null ? account.getCurrency() : "EUR");

            allocations.add(allocation);
        }

        BigDecimal totalValue = allocations.stream()
                .map(AssetAllocation::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Weight is meaningless if total is zero — skip setting it
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (AssetAllocation a : allocations) {
                BigDecimal weight = a.getCurrentValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                a.setWeightPct(weight);
            }
        }

        // Asset allocation represents current state — delete and replace on every sync
        repository.deleteAllByUserId(userId);
        repository.saveAll(allocations);

        log.info("Asset allocation sync complete for user {} — {} accounts", userId, allocations.size());
    }

    // --- Queries ---

    public List<AssetAllocationResponseDTO> getAll(UUID userId) {
        return repository.findByUserIdOrderByWeightPctDesc(userId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }
}