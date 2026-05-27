package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.BankConnectionUpdateDTO;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BankConnectionMapper {

    //Shared date format - all mappers in this service use this constant
    //Frontend always receives ISO-8601, never raw toString
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    //Entity => ResponseDTO
    public BankConnectionResponseDTO toResponseDTO(BankConnection connection) {
        BankConnectionResponseDTO response = new BankConnectionResponseDTO();
        response.setId(connection.getId().toString());
        response.setUserId(connection.getUserId().toString());
        response.setInstitutionId(connection.getInstitutionId());
        response.setInstitutionName(connection.getInstitutionName());
        response.setTinkUserId(connection.getTinkUserId());
        response.setCredentialsId(connection.getCredentialsId());
        response.setStatus(connection.getStatus().name());
        response.setExpiresAt(connection.getExpiresAt() != null
                ? connection.getExpiresAt().format(DATE_FORMAT) : null);
        response.setLastSyncedAt(connection.getLastSyncedAt() != null
                ? connection.getLastSyncedAt().format(DATE_FORMAT) : null);
        response.setCreatedAt(connection.getCreatedAt() != null
                ? connection.getCreatedAt().format(DATE_FORMAT) : null);
        response.setUpdatedAt(connection.getUpdatedAt() != null
                ? connection.getUpdatedAt().format(DATE_FORMAT) : null);
        return response;
    }

    //CreateDTO => Entity
    //tinkUserId and credentialsId are set internally by TinkService after auth — not provided by client
    public BankConnection toEntity(BankConnectionCreateDTO request, UUID userId) {
        BankConnection connection = new BankConnection();
        connection.setUserId(userId);
        connection.setInstitutionId(request.getInstitutionId());
        connection.setInstitutionName(request.getInstitutionName());
        connection.setStatus(ConnectionStatus.PENDING);
        return connection;
    }

    //UpdateDTO => Entity
    public BankConnection applyUpdate(BankConnection existing, BankConnectionUpdateDTO request) {
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getExpiresAt() != null) {
            existing.setExpiresAt(LocalDateTime.parse(request.getExpiresAt(), DATE_FORMAT));
        }
        return existing;
    }
}