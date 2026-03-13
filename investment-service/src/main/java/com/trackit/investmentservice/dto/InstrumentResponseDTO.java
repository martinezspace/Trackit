package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentResponseDTO {

    private String id;
    private String isin;
    private String ticker;
    private String name;
    private String instrumentType;
    private String currency;
    private String exchange;
    private String country;
    private String createdAt;
}
