package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.BankAccountUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.BankAccountMapper;
import com.trackit.bankaccountservice.model.BankAccount;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.repository.BankAccountRepository;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final BankConnectionRepository connectionRepository;
    private final BankAccountMapper accountMapper;

    //Queries
    public List<BankAccountResponseDTO> getAllAccountsForUser(UUID userId) {
        return accountRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(accountMapper::toResponseDTO)
                .toList();
    }

    public BankAccountResponseDTO getAccountById(UUID id, UUID userId) {
        BankAccount account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        return accountMapper.toResponseDTO(account);
    }

    //Commands
    public BankAccountResponseDTO createAccount(UUID userId, BankAccountCreateDTO request) {
        //Verify the connection exists and belongs to this user
        BankConnection connection = connectionRepository.findByIdAndUserId(request.getConnectionId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + request.getConnectionId()));

        //Idempotency - skip if this external account was already imported
        return accountRepository.findByConnectionIdAndExternalAccountId(
                connection.getId(), request.getExternalAccountId())
                .map(accountMapper::toResponseDTO)
                .orElseGet(() -> accountMapper.toResponseDTO(accountRepository.save(accountMapper.toEntity(request, connection, userId))));
    }

    public BankAccountResponseDTO updateAccount(UUID id, UUID userId, BankAccountUpdateDTO request) {
        BankAccount existing = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        BankAccount updated = accountMapper.applyUpdate(existing, request);
        return accountMapper.toResponseDTO(accountRepository.save(updated));
    }

    public BankAccountResponseDTO updateBalance(UUID id, UUID userId, BigDecimal current, BigDecimal available) {
        BankAccount account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        account.setCurrentBalance(current);
        account.setAvailableBalance(available);
        account.setBalanceUpdatedAt(LocalDateTime.now());
        return accountMapper.toResponseDTO(accountRepository.save(account));
    }

    public void deactiveAccount(UUID id, UUID userId) {
        BankAccount existing = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        //Soft delete - preserve transaction history
        existing.setActive(false);
        accountRepository.save(existing);
    }
}
