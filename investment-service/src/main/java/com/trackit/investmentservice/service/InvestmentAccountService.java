package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InvestmentAccountCreateDTO;
import com.trackit.investmentservice.dto.InvestmentAccountResponseDTO;
import com.trackit.investmentservice.dto.InvestmentAccountUpdateDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.InvestmentAccountMapper;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InvestmentAccountService {
    private final InvestmentAccountRepository investmentAccountRepository;
    private final InvestmentAccountMapper investmentAccountMapper;

    //Queries
    public List<InvestmentAccountResponseDTO> getAllAccountsForUser(UUID userId) {
        return investmentAccountRepository.findByUserId(userId)
                .stream()
                .map(investmentAccountMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public InvestmentAccountResponseDTO getAccountById(UUID id, UUID userId) {
        InvestmentAccount account = investmentAccountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        return investmentAccountMapper.toResponseDTO(account);
    }

    //Commands
    public InvestmentAccountResponseDTO createAccount(UUID userId, InvestmentAccountCreateDTO request) {
        InvestmentAccount account = investmentAccountMapper.toEntity(request, userId);
        return investmentAccountMapper.toResponseDTO(investmentAccountRepository.save(account));
    }

    public InvestmentAccountResponseDTO updateAccount(UUID id, UUID userId, InvestmentAccountUpdateDTO request) {
        InvestmentAccount existing = investmentAccountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        InvestmentAccount updated = investmentAccountMapper.applyUpdate(existing, request);
        return investmentAccountMapper.toResponseDTO(investmentAccountRepository.save(updated));
    }

    public void deactiveAccount(UUID id, UUID userId) {
        InvestmentAccount existing = investmentAccountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found " + id));
        existing.setActive(false);
        investmentAccountRepository.save(existing);
    }
}
