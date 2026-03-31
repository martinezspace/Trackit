package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    //Latest price for an instrument - used after fetching from Alpha Vantage
    //to update holdings current_price
    Optional<PriceHistory> findTopByInstrument_IdOrderByPriceDateDesc(UUID instrumentId);

    //Price history for a date range - used for portfolio charts
    // e.g. last 30 days, last 90, last year
    List<PriceHistory> findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
            UUID instrumentId, LocalDate from, LocalDate to
    );

    //Check if price already exists for this instrument on this date
    //Prevents duplicate inserts if PriceWorker runs twice
    boolean existsByInstrument_IdAndPriceDate(UUID instrumentId, LocalDate priceDate);
}
