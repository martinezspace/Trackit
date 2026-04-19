package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.BankConnectionUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.BankConnectionMapper;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BankConnectionService {

    private final BankConnectionRepository bankConnectionRepository;
    private final BankConnectionMapper bankConnectionMapper;

    //Queries
    public List<BankConnectionResponseDTO> getAllConnectionsForUser(UUID userId) {
        return bankConnectionRepository.findByUserId(userId)
                .stream()
                .map(bankConnectionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public BankConnectionResponseDTO getConnectionById(UUID id, UUID userId) {
        BankConnection connection = bankConnectionRepository.findByIdAndUserId(id ,userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + id));
        return bankConnectionMapper.toResponseDTO(connection);
    }

    //Commands
    public BankConnectionResponseDTO createConnection(UUID userId, BankConnectionCreateDTO request) {
        BankConnection connection = bankConnectionMapper.toEntity(request, userId);
        return bankConnectionMapper.toResponseDTO(bankConnectionRepository.save(connection));
    }

    public BankConnectionResponseDTO updateConnection(UUID id, UUID userId, BankConnectionUpdateDTO request) {
        BankConnection existing = bankConnectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + id));
        BankConnection updated = bankConnectionMapper.applyUpdate(existing, request);
        return bankConnectionMapper.toResponseDTO(bankConnectionRepository.save(updated));
    }

    public void deleteConnection(UUID id, UUID userId) {
        BankConnection existing = bankConnectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + id));
        bankConnectionRepository.delete(existing);
    }
}
