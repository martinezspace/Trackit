package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.InvestmentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, UUID> {

    // All accounts for a user
    List<InvestmentAccount> findByUserId(UUID userId);

    // Active accounts only - used for holding and price enrichment
    List<InvestmentAccount> findByUserIdAndActiveTrue(UUID userId);

    //Ownership check - always verify account belongs to this user before operation
    Optional<InvestmentAccount> findByIdAndUserId(UUID id, UUID userId);
}
