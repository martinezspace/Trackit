package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InvestmentTransactionResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.InvestmentTransactionMapper;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.model.InvestmentTransaction;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import com.trackit.investmentservice.repository.InvestmentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentTransactionService {

    private final InvestmentTransactionRepository transactionRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final InvestmentTransactionMapper transactionMapper;

    //Queries
    //All active transactions for an account - transaction history view
    public List<InvestmentTransactionResponseDTO> getAllTransactionsForAccount(UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        return transactionRepository.findByAccount_IdAndCancelledFalse(accountId)
                .stream()
                .map(transactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Transactions for a specific instrument in an account
    public List<InvestmentTransactionResponseDTO> getTransactionsByInstrument(
            UUID accountId, UUID instrumentId, UUID userId
    ) {
        verifyAccountOwnership(accountId, userId);
        return transactionRepository.findByAccount_IdAndInstrument_IdAndCancelledFalse(accountId, instrumentId)
                .stream()
                .map(transactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Single transaction by id
    public InvestmentTransactionResponseDTO getTransactionById(UUID id, UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        InvestmentTransaction transaction = transactionRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction Not Found: " + id));
        return transactionMapper.toResponseDTO(transaction);
    }

    //Commands
    //Cancel a single transaction - soft delete
    public InvestmentTransactionResponseDTO cancelTransaction(UUID id, UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        InvestmentTransaction transaction = transactionRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction Not Found: " + id));

        if (transaction.isCancelled()) {
            throw new IllegalStateException("Transaction is already cancelled");
        }

        transaction.setCancelled(true);
        return transactionMapper.toResponseDTO(transactionRepository.save(transaction));
    }

    //Cancel all transactions from a batch - called when batch is cancelled
    //Internal method - not exposed to controller
    public void cancelAllTransactionsForBatch(UUID batchId) {
        List<InvestmentTransaction> transactions = transactionRepository.findByBatch_Id(batchId);

        if (transactions.isEmpty()) {
            return;
        }

        transactions.forEach(t -> t.setCancelled(true));
        transactionRepository.saveAll(transactions);
    }

    //Helper
    private void verifyAccountOwnership(UUID accountId, UUID userId) {
        investmentAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found" + accountId));
    }
}
