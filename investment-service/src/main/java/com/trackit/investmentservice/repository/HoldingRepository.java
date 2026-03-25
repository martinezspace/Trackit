package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    //All holdings for an account - portfolio view
    List<Holding> findByAccount_Id(UUID accountId);

    //Single holding by account and instrument - unique pair
    //Used during recalculation to find existing holding or confirm absence
    Optional<Holding> findByAccount_IdAndInstrument_Id(UUID accountId, UUID instrumentId);

    //Ownership check - verify holding belongs to user's account
    Optional<Holding> findByIdAndAccount_Id(UUID id, UUID accountId);

    //All holdings for a specific instrument across all accounts
    //Used by PriceWorker to update current_price when new price is fetched
    List<Holding> findByInstrument_Id(UUID instrumentId);
}
