package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {

    //All connections for a user
    List<BankConnection> findByUserId(UUID userId);

    //Ownership check - always verify account belongs to this user before operation
    Optional<BankConnection> findByIdAndUserId(UUID id, UUID userId);

    // Webhook lookup - GoCardless identifies callbacks by requisitionId, not our internal ID
    Optional<BankConnection> findByRequisitionId(String requisitionId);
}
