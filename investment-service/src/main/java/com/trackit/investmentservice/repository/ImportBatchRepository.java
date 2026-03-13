package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.ImportBatch;
import com.trackit.investmentservice.model.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    //All batches for an account - newest first
    //Used to show import history on the account page
    List<ImportBatch> findByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    //Singel batch by id scoped to account, ownership check
    //Used for status polling and cancel requests
    Optional<ImportBatch> findByIdAndAccount_Id(UUID id, UUID accountId);

    //Find by status, useful for reprocessing stuck PENDING or PROCESSING batches - basically for admin side
    List<ImportBatch> findByStatus(ImportStatus status);

    //Check if account has any active imports - prevent concurrent imports
    boolean existsByAccount_IdAndStatusIn(UUID accountId, List<ImportStatus> statuses);
}
