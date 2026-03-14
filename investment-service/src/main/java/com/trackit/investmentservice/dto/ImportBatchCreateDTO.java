package com.trackit.investmentservice.dto;

import com.trackit.investmentservice.model.BrokerFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ImportBatchCreateDTO {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Broker format is required")
    private BrokerFormat brokerFormat;

    @NotBlank(message = "filename is required")
    @Size(max = 255)
    private String filename;
}
