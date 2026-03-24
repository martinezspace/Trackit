package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.ImportBatchCreateDTO;
import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.ImportBatchMapper;
import com.trackit.investmentservice.model.ImportBatch;
import com.trackit.investmentservice.model.ImportStatus;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.repository.ImportBatchRepository;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final InvestmentTransactionService transactionService;
    private final ImportBatchMapper importBatchMapper;

    //Queries

    //All batches for an account - newest first
    public List<ImportBatchResponseDTO> getAllBatchesForAccount(UUID accountId, UUID userId) {
        //Verify account belongs to user
        verifyAccountOwnership(accountId, userId);
        return importBatchRepository.findByAccount_IdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(importBatchMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Single batch - used for status polling
    public ImportBatchResponseDTO getBatchById(UUID batchId, UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        ImportBatch batch = importBatchRepository.findByIdAndAccount_Id(batchId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Import Batch Not Found: " + batchId));
        return importBatchMapper.toResponseDTO(batch);
    }

    //Commands
    public ImportBatchResponseDTO createBatch(ImportBatchCreateDTO request, UUID userId) {
        InvestmentAccount account = verifyAccountOwnership(request.getAccountId(), userId);

        //Prevent concurrent imports on same account
        boolean hasActiveImport = importBatchRepository.existsByAccount_IdAndStatusIn(
                request.getAccountId(),
                List.of(ImportStatus.PENDING, ImportStatus.PROCESSING)
        );
        if (hasActiveImport) {
            throw new IllegalStateException("An import is already in progress for this account");
        }

        //Build and save batch - status defaults to PENDING
        ImportBatch batch = new ImportBatch();
        batch.setAccount(account);
        batch.setBrokerFormat(request.getBrokerFormat());
        batch.setFilename(request.getFilename());

        return importBatchMapper.toResponseDTO(importBatchRepository.save(batch));
    }

    @Transactional
    public ImportBatchResponseDTO cancelBatch(UUID batchId, UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);

        ImportBatch batch = importBatchRepository.findByIdAndAccount_Id(batchId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Import Batch Not Found" + batchId));

        //Can only cancel PENDING or PROCESSING batches
        if (batch.getStatus() == ImportStatus.COMPLETED
                || batch.getStatus() == ImportStatus.FAILED
                || batch.getStatus() == ImportStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a batch with status: " + batch.getStatus());
        }

        //Cancel all transactions from batch first
        transactionService.cancelAllTransactionsForBatch(batchId);

        //Then cancel the batch itself
        batch.setStatus(ImportStatus.CANCELLED);
        return importBatchMapper.toResponseDTO(importBatchRepository.save(batch));
    }

    //Helper
    private InvestmentAccount verifyAccountOwnership(UUID accountId, UUID userId) {
        return investmentAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found: " + accountId));
    }
}
