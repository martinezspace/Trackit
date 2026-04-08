package com.trackit.investmentservice.dto;

import com.trackit.investmentservice.model.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentCreateDTO {

    //Nullable - PPK funds and some internal instruments don't have an ISIN
    @Size(min = 12, max = 12, message = "ISIN must be exactly 12 characters")
    private String isin;

    //Nullable - some instruments may not have a ticker
    @Size(max = 20)
    private String ticker;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Instrument type is required")
    private InstrumentType instrumentType;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code e.g. PLN, EUR, USD")
    private String currency;

    //Nullable - some instruments may not have a listed exchange
    @Size(max = 50)
    private String exchange;

    //Nullable e.g. PL, DE
    @Size(min = 2, max = 2, message = "Country must be a 2-letter code e.g PL, DE")
    private String country;
}
