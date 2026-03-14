package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportBatchResponseDTO {

    private String id;
    private String accountId;
    private String brokerFormat;
    private String filename;
    private String status;

    //Nullable until processing starts
    private Integer rowCount;
    private Integer importedCount;
    private Integer errorCount;

    //Nullable - populated when rows fail
    private String errorDetails;

    //Nullable - populated when import finishes
    private String importedAt;

    private String createdAt;
}
