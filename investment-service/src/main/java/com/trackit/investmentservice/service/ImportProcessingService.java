package com.trackit.investmentservice.service;

import com.trackit.investmentservice.csv.CsvParser;
import com.trackit.investmentservice.csv.ParsedTransaction;
import com.trackit.investmentservice.csv.Trading212CsvParser;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.ImportBatchRepository;
import com.trackit.investmentservice.repository.InstrumentRepository;
import com.trackit.investmentservice.repository.InvestmentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImportProcessingService.class);

    private final FileStorageService fileStorageService;
    private final ImportBatchRepository importBatchRepository;
    private final InstrumentRepository instrumentRepository;
    private final InvestmentTransactionRepository transactionRepository;
    private final HoldingService holdingService;
    private final Trading212CsvParser trading212CsvParser;

    //Runs in background thread - called from ImportService
    //@Async works correctly because it's called outside the class
    @Async
    public void processImportAsync(UUID batchId, UUID accountId, BrokerFormat brokerFormat, String s3Key) {
        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        //Update status to PROCESSING
        batch.setStatus(ImportStatus.PROCESSING);
        importBatchRepository.save(batch);

        try {
            //Download CSV from S3
            InputStream csvStream = fileStorageService.downloadFile(s3Key);

            //Pick correct parser based on broker format
            CsvParser parser = selectParser(brokerFormat);

            //Parse CSV => list of raw transactions
            List<ParsedTransaction> parsedTransactions = parser.parse(csvStream);
            batch.setRowCount(parsedTransactions.size());

            //Process each parsed transaction
            int importedCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();

            for (ParsedTransaction parsed : parsedTransactions) {
                try {
                    processTransaction(parsed, batch);
                    importedCount++;
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Row " + (importedCount + errorCount) + ": " + e.getMessage());
                    log.warn("Failed to process transaction - externalId: {}, reason: {}", parsed.getExternalId(), e.getMessage());
                }
            }

            //Update batch with results
            batch.setImportedCount(importedCount);
            batch.setErrorCount(errorCount);
            batch.setStatus(ImportStatus.COMPLETED);
            batch.setImportedAt(LocalDateTime.now());

            if (!errors.isEmpty()) {
                batch.setErrorDetails(buildErrorDetails(errors));
            }

            importBatchRepository.save(batch);
            log.info("Import completed - batchId: {}, imported: {}, errors: {}", batchId, importedCount, errorCount);

            //Recalculate holdings after all transactions are created
            holdingService.recalculateHoldings(accountId);
        } catch (Exception e) {
            batch.setStatus(ImportStatus.FAILED);
            batch.setErrorDetails("{\"error\": \"" + e.getMessage() + "\"}");
            importBatchRepository.save(batch);
            log.error("Import failed - batchId: {}, reason: {}", batchId, e.getMessage());
        }
    }

    //helper
    @Transactional
    private void processTransaction(ParsedTransaction parsed, ImportBatch batch) {
        //Skip duplicate transactions - same external ID already imported for this account
        UUID accountId = batch.getAccount().getId();
        if (transactionRepository.existsByAccount_IdAndExternalId(accountId, parsed.getExternalId())) {
            log.debug("Skipping duplicate transaction: {}", parsed.getExternalId());
            return;
        }

        //Find or create instrument by ISIN
        Instrument instrument = instrumentRepository.findByIsin(parsed.getIsin())
                .orElseGet(() -> createInstrument(parsed));

        //Map transaction type
        TransactionType transactionType = TransactionType.valueOf(parsed.getTransactionType());

        //Create and save transaction
        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setAccount(batch.getAccount());
        transaction.setBatch(batch);
        transaction.setInstrument(instrument);
        transaction.setExternalId(parsed.getExternalId());
        transaction.setTransactionType(transactionType);
        transaction.setQuantity(parsed.getQuantity());
        transaction.setPrice(parsed.getPrice());
        transaction.setAmount(parsed.getAmount());
        transaction.setCurrency(parsed.getCurrency());
        transaction.setTransactionDate(parsed.getTransactionDate());
        transaction.setCancelled(false);

        transactionRepository.save(transaction);
    }

    private Instrument createInstrument(ParsedTransaction parsed) {
        Instrument instrument = new Instrument();
        instrument.setIsin(parsed.getIsin());
        instrument.setTicker(parsed.getTicker());
        instrument.setName(parsed.getInstrumentName());
        instrument.setCurrency(parsed.getCurrency());
        //Default to stock can be refined later
        instrument.setInstrumentType(InstrumentType.STOCK);
        return instrumentRepository.save(instrument);
    }

    private CsvParser selectParser(BrokerFormat brokerFormat) {
        return switch (brokerFormat) {
            case TRADING212 -> trading212CsvParser;
            default -> throw new IllegalArgumentException("Unsupported broker format: " + brokerFormat);
        };
    }

    private String buildErrorDetails(List<String> errors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i =0; i < errors.size(); i++) {
            sb.append("\"").append(errors.get(i).replace("\"", "'")).append("\"");
            if (i < errors.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
