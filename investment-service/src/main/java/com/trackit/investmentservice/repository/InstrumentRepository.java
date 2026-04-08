package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {

    //Lookup by ISIN - priary way to find an instrument
    //Used during CSV import to check if instrument already exists
    Optional<Instrument> findByIsin(String isin);
    //Lookup by name - used for PPK funds and instruments without ISIN
    Optional<Instrument> findByName(String name);
    //Lookup by ticker - secondary, nullable so Optional
    Optional<Instrument> findByTicker(String ticker);

    //Filter by type
    List<Instrument> findByInstrumentType(InstrumentType instrumentType);

    //Existence checks
    boolean existsByIsin(String isin);
    boolean existsByName(String name);

}
