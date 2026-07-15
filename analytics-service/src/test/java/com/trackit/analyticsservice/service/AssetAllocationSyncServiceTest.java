package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.InvestmentServiceClient;
import com.trackit.analyticsservice.dto.client.investment.HoldingDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.response.AssetAllocationResponseDTO;
import com.trackit.analyticsservice.mapper.AssetAllocationMapper;
import com.trackit.analyticsservice.model.AssetAllocation;
import com.trackit.analyticsservice.repository.AssetAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AssetAllocationSyncServiceTest {

    @Mock private AssetAllocationRepository repository;
    @Mock private AssetAllocationMapper mapper;
    @Mock private InvestmentServiceClient investmentClient;

    @InjectMocks
    private AssetAllocationSyncService service;

    private UUID userId;
    private UUID accountAId;
    private UUID accountBId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        accountAId = UUID.randomUUID();
        accountBId = UUID.randomUUID();
    }

    // sync

    @Test
    void sync_deletesExistingAndSavesNew() {
        when(investmentClient.getInvestmentAccounts(userId))
                .thenReturn(List.of(buildAccount(accountAId, "BROKERAGE", true)));
        when(investmentClient.getHoldings(accountAId, userId))
                .thenReturn(List.of(buildHolding("1000.00")));

        service.sync(userId);

        verify(repository).deleteAllByUserId(userId);
        verify(repository).saveAll(anyList());
    }

    @Test
    void sync_calculatesWeightsCorrectly() {
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of(
                buildAccount(accountAId, "BROKERAGE", true),
                buildAccount(accountBId, "PENSION", true)
        ));
        when(investmentClient.getHoldings(accountAId, userId))
                .thenReturn(List.of(buildHolding("6000.00")));
        when(investmentClient.getHoldings(accountBId, userId))
                .thenReturn(List.of(buildHolding("4000.00")));

        service.sync(userId);

        ArgumentCaptor<List<AssetAllocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<AssetAllocation> saved = captor.getValue();

        assertThat(saved).hasSize(2);
        AssetAllocation brokerageAlloc = saved.stream()
                .filter(a -> a.getAccountId().equals(accountAId)).findFirst().orElseThrow();
        AssetAllocation pensionAlloc = saved.stream()
                .filter(a -> a.getAccountId().equals(accountBId)).findFirst().orElseThrow();

        assertThat(brokerageAlloc.getWeightPct()).isEqualByComparingTo("60.0000");
        assertThat(pensionAlloc.getWeightPct()).isEqualByComparingTo("40.0000");
    }

    @Test
    void sync_skipsInactiveAccounts() {
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of(
                buildAccount(accountAId, "BROKERAGE", true),
                buildAccount(accountBId, "PENSION", false)
        ));
        when(investmentClient.getHoldings(accountAId, userId))
                .thenReturn(List.of(buildHolding("1000.00")));

        service.sync(userId);

        ArgumentCaptor<List<AssetAllocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(investmentClient, never()).getHoldings(eq(accountBId), any());
    }

    @Test
    void sync_skipsHoldingsWithNullCurrentValue() {
        when(investmentClient.getInvestmentAccounts(userId))
                .thenReturn(List.of(buildAccount(accountAId, "BROKERAGE", true)));
        when(investmentClient.getHoldings(accountAId, userId))
                .thenReturn(List.of(buildHolding(null), buildHolding("500.00")));

        service.sync(userId);

        ArgumentCaptor<List<AssetAllocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getCurrentValue()).isEqualByComparingTo("500.00");
    }

    @Test
    void sync_doesNotSetWeights_whenTotalValueIsZero() {
        when(investmentClient.getInvestmentAccounts(userId))
                .thenReturn(List.of(buildAccount(accountAId, "BROKERAGE", true)));
        when(investmentClient.getHoldings(accountAId, userId))
                .thenReturn(List.of(buildHolding(null)));

        service.sync(userId);

        ArgumentCaptor<List<AssetAllocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getWeightPct()).isNull();
    }

    // getAll

    @Test
    void getAll_returnsMappedList() {
        AssetAllocation allocation = new AssetAllocation();
        AssetAllocationResponseDTO dto = new AssetAllocationResponseDTO();

        when(repository.findByUserIdOrderByWeightPctDesc(userId)).thenReturn(List.of(allocation));
        when(mapper.toResponseDTO(allocation)).thenReturn(dto);

        assertThat(service.getAll(userId)).containsExactly(dto);
    }

    @Test
    void getAll_returnsEmptyList_whenNoAllocations() {
        when(repository.findByUserIdOrderByWeightPctDesc(userId)).thenReturn(List.of());

        assertThat(service.getAll(userId)).isEmpty();
    }

    private InvestmentAccountDTO buildAccount(UUID id, String type, boolean active) {
        InvestmentAccountDTO dto = new InvestmentAccountDTO();
        dto.setId(id.toString());
        dto.setAccountType(type);
        dto.setDisplayName(type + " Account");
        dto.setCurrency("EUR");
        dto.setActive(active);
        return dto;
    }

    private HoldingDTO buildHolding(String currentValue) {
        HoldingDTO dto = new HoldingDTO();
        dto.setCurrentValue(currentValue);
        dto.setCurrency("EUR");
        return dto;
    }
}