package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.model.ImportBatch;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchMapper {

    //Entity -> ResponseDTO
    public ImportBatchResponseDTO toResponseDTO(ImportBatch batch) {
        ImportBatchResponseDTO response = new ImportBatchResponseDTO();
        response.setId(batch.getId().toString());
        response.setAccountId(batch.getAccount().getId().toString());
        response.setBrokerFormat(batch.getBrokerFormat().name());
        response.setFilename(batch.getFilename());
        response.setStatus(batch.getStatus().name());
        response.setRowCount(batch.getRowCount());
        response.setImportedCount(batch.getImportedCount());
        response.setErrorCount(batch.getErrorCount());
        response.setErrorDetails(batch.getErrorDetails());
        response.setImportedAt(batch.getImportedAt() != null
                ? batch.getImportedAt().toString()
                : null);
        response.setCreatedAt(batch.getCreatedAt() != null
                ? batch.getCreatedAt().toString()
                : null);
        return response;
    }
    //toEntity doesn't exists, logic will belong in the service
}
