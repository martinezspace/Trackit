package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //Nullable - ppk funds and some internal instruments dont have an ISIN
    //ISIN is always exactly 12 characters
    @Size(min = 12, max = 12, message = "ISIN must be exactly 12 characters")
    @Column(name = "isin", unique = true, length = 12)
    private String isin;

    //Nullable - some instruments may not have a ticker
    @Size(max = 20)
    @Column(name = "ticker", length = 20)
    private String ticker;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    //Nullable - PPK funds may not have a listed exchange
    @Size(max = 50)
    @Column(name = "exchange", length = 50)
    private String exchange;

    //Nullable e.g. PL, US, DE
    @Size(min = 2, max = 2)
    @Column(name = "country", length = 2)
    private String country;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
