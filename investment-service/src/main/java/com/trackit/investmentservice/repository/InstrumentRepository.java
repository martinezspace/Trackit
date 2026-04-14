package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    //Only tickers with active holdings (quantity > 0)
    //Used by PriceWorker - no point fetching prices for instruments nobody holds anymore
    @Query("SELECT DISTINCT i FROM Instrument i " +
            "JOIN Holding h ON h.instrument.id = i.id " +
            "WHERE i.ticker IS NOT NULL " +
            "AND h.quantity > 0")
    List<Instrument> findInstrumentsWithActiveHoldings();
}
