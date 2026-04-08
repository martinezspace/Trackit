package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.model.BrokerFormat;
import com.trackit.investmentservice.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "CSV and XLS file import for investment transactions")
public class ImportController {

    private final ImportService importService;

    //POST /api/import
    //Accepts multipart/form-data - file upload + metadata
    //Returns immediately with batch details - processing happens in background
    @Operation(
            summary = "Upload file to import",
            description = "Uploads CSV or XLS file to S3 and triggers async processing." +
                    "Returns immediatey with batchId - poll GET /api/import-batches/{id} for status"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResponseDTO> importCsv(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("accountId") UUID accountId,
            @RequestParam("brokerFormat") BrokerFormat brokerFormat,
            @RequestPart("file") MultipartFile file
    ) {
       if (file.isEmpty()) {
           throw new IllegalArgumentException("File is empty");
       }

        if (!isSupportedFile(file)) {
            throw new IllegalArgumentException("Unsupported file type.");
        }

        return ResponseEntity
                .status(HttpStatus.ACCEPTED) //202 - accepted for processing, not yet complete
                .body(importService.initiateImport(userId, accountId, brokerFormat, file));
    }

    //helper
    private boolean isSupportedFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".csv") ||
                lower.endsWith(".xls") ||
                lower.endsWith(".xlsx") ||
                "text/csv".equals(file.getContentType()) ||
                "application/vnd.ms-excel".equals(file.getContentType()) ||
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        .equals(file.getContentType());
    }
}
