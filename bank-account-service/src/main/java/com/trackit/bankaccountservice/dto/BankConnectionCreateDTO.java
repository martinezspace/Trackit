package com.trackit.bankaccountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BankConnectionCreateDTO {

    @NotBlank(message = "Institution ID is required")
    @Size(max = 100, message = "Institution ID must be 100 characters or less")
    private String institutionId;

    @NotBlank(message = "Institution name is required")
    @Size(max = 255, message = "Institution name must be 255 characters or less")
    private String institutionName;

    //Provided by GoCardless after creating a requistion on their API
    //Required because we need it to correlate their webhook call
    @NotBlank(message = "Requisition ID is required")
    @Size(max = 255, message = "Requisition ID must be 255 characters or less")
    private String requisitionId;
}
