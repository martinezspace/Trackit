package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository  extends JpaRepository<BankAccount, UUID> {

    //Active accounts only - deactivated accounts are hidden from the user
    List<BankAccount> findByUserIdAndActiveTrue(UUID userId);

    //All accounts under a connection - used when syncing or revoking a connection
    List<BankAccount> findByConnectionId(UUID connectionId);

    //Ownership check - always verify account belongs to this user before any operation
    Optional<BankAccount> findByIdAndUserId(UUID id, UUID userId);

    //Idempotency check during import - prevents duplicate accounts from GoCardless
    Optional<BankAccount> findByConnectionIdAndExternalAccountId(UUID connectionId, String externalAccountId);
}
