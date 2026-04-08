package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.ImportBatchMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.ImportBatchRepository;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final FileStorageService fileStorageService;
    private final ImportBatchRepository importBatchRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final ImportBatchMapper importBatchMapper;
    private final ImportProcessingService importProcessingService;

    //step 1 - upload file, create batch return immediately
    //Called directly by controller, returns before processing starts
    public ImportBatchResponseDTO initiateImport(
            UUID userId, UUID accountId, BrokerFormat brokerFormat, MultipartFile file) {
        //Verify account belongs to user
        InvestmentAccount account = investmentAccountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found" + accountId));

        //Prevent concurrent imports on same account
        boolean hasActiveImport = importBatchRepository.existsByAccount_IdAndStatusIn(
                accountId, List.of(ImportStatus.PENDING, ImportStatus.PROCESSING));
        if (hasActiveImport) {
            throw new IllegalStateException("An import is already in progress for this account");
        }

        //Generate batchId upfront so we can use it in the S3 key
        UUID batchId = UUID.randomUUID();

        //Upload file to S3
        String s3Key = fileStorageService.uploadFile(file, userId, batchId);

        //Create batch record, status PENDING
        ImportBatch batch = new ImportBatch();
        batch.setAccount(account);
        batch.setBrokerFormat(brokerFormat);
        batch.setFilename(file.getOriginalFilename());
        batch.setS3Key(s3Key);
        batch.setStatus(ImportStatus.PENDING);
        ImportBatch savedBatch = importBatchRepository.save(batch);

        //Trigger async processing, returns immediately
        importProcessingService.processImportAsync(savedBatch.getId(), accountId, brokerFormat, s3Key);

        log.info("Import initiated - batchId: {}, account: {}", savedBatch.getId(), accountId);
        return importBatchMapper.toResponseDTO(savedBatch);
    }
}
