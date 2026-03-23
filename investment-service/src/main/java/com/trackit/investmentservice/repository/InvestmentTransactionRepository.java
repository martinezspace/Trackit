package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.InvestmentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, UUID> {

    //All Active transactions for an account - transaction history view
    //Excludes cancelled transactions
    List<InvestmentTransaction> findByAccount_IdAndCancelledFalse(UUID accountId);

    //Transactions for a specific instrument in an account
    //Used when showing transaction history per instrument
    List<InvestmentTransaction> findByAccount_IdAndInstrument_IdAndCancelledFalse(UUID accountId, UUID instrumentId);

    //Ownership check - verify transaction belongs to users account
    Optional<InvestmentTransaction> findByIdAndAccount_Id(UUID id, UUID accountId);

    //Duplicate check during CSV import
    //Returns true if this external_id already exists for this account
    boolean existsByAccount_IdAndExternalId(UUID accountId, String externalId);

    //All transactions from a specific batch
    //Used when cancelling a batch - mark all its transactions as cancelled
    List<InvestmentTransaction> findByBatch_Id(UUID batchId);
}
