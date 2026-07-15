package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.AssetAllocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AssetAllocationRepositoryTest {

    @Autowired
    private AssetAllocationRepository repository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        repository.save(buildAllocation(userId, "Stocks Account", "BROKERAGE", "60.0000"));
        repository.save(buildAllocation(userId, "Pension Account", "PENSION", "30.0000"));
        repository.save(buildAllocation(userId, "ETF Account", "ETF", "10.0000"));
        repository.save(buildAllocation(otherUserId, "Other Account", "BROKERAGE", "100.0000"));
    }

    // findByUserIdOrderByWeightPctDesc

    @Test
    void findByUserIdOrderByWeightPctDesc_returnsOnlyUserAllocations() {
        List<AssetAllocation> result = repository.findByUserIdOrderByWeightPctDesc(userId);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(a -> a.getUserId().equals(userId));
    }

    @Test
    void findByUserIdOrderByWeightPctDesc_returnsOrderedByWeightDesc() {
        List<AssetAllocation> result = repository.findByUserIdOrderByWeightPctDesc(userId);

        assertThat(result.get(0).getAccountDisplayName()).isEqualTo("Stocks Account");
        assertThat(result.get(1).getAccountDisplayName()).isEqualTo("Pension Account");
        assertThat(result.get(2).getAccountDisplayName()).isEqualTo("ETF Account");
    }

    @Test
    void findByUserIdOrderByWeightPctDesc_returnsEmpty_whenNoAllocations() {
        List<AssetAllocation> result = repository.findByUserIdOrderByWeightPctDesc(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // deleteAllByUserId

    @Test
    void deleteAllByUserId_removesOnlyUserAllocations() {
        repository.deleteAllByUserId(userId);

        assertThat(repository.findByUserIdOrderByWeightPctDesc(userId)).isEmpty();
        // Other user's allocations unaffected
        assertThat(repository.findByUserIdOrderByWeightPctDesc(otherUserId)).hasSize(1);
    }

    @Test
    void deleteAllByUserId_doesNothing_whenNoAllocationsExist() {
        repository.deleteAllByUserId(UUID.randomUUID());

        assertThat(repository.findAll()).hasSize(4);
    }

    private AssetAllocation buildAllocation(UUID userId, String displayName, String type, String weight) {
        AssetAllocation a = new AssetAllocation();
        a.setUserId(userId);
        a.setAccountId(UUID.randomUUID());
        a.setAccountDisplayName(displayName);
        a.setAccountType(type);
        a.setInstrumentType(type);
        a.setCurrentValue(new BigDecimal("10000.00"));
        a.setWeightPct(new BigDecimal(weight));
        a.setCurrency("EUR");
        return a;
    }
}