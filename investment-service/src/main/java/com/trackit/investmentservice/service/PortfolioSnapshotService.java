package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.PortfolioSnapshotCreateDTO;
import com.trackit.investmentservice.dto.PortfolioSnapshotResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.PortfolioSnapshotMapper;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.model.PortfolioSnapshot;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import com.trackit.investmentservice.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final PortfolioSnapshotMapper snapshotMapper;

    //Queries

    //Full portfolio history - all snapshots oldest to newest
    public List<PortfolioSnapshotResponseDTO> getAllSnapshots(UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        return snapshotRepository.findByAccount_IdOrderBySnapshotDateAsc(accountId)
                .stream()
                .map(snapshotMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Date range - used for 1M, 3M, 1Y chart views
    public List<PortfolioSnapshotResponseDTO> getSnapshotByDateRange(
            UUID accountId, UUID userId, LocalDate from, LocalDate to) {
        verifyAccountOwnership(accountId, userId);
        return snapshotRepository
                .findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(accountId, from, to)
                .stream()
                .map(snapshotMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Latest snapshot - current portfolio summry card on dashboard
    public PortfolioSnapshotResponseDTO getLatestSnapshot(UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        PortfolioSnapshot snapshot = snapshotRepository
                .findTopByAccount_IdOrderBySnapshotDateDesc(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Snapshots Found For Account: " + accountId));
        return snapshotMapper.toResponseDTO(snapshot);
    }

    //Command
    //Save a new snapshot - called by PriceWorker after updating all prices
    //Skips if snapshot already exists for today
    public PortfolioSnapshotResponseDTO saveSnapshot(PortfolioSnapshotCreateDTO request) {
        InvestmentAccount account = investmentAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found: " + request.getAccountId()));

        //Skip if snapshot already exists for this date
        if (snapshotRepository.existsByAccount_IdAndSnapshotDate(request.getAccountId(), request.getSnapshotDate())) {
            return snapshotRepository.findTopByAccount_IdOrderBySnapshotDateDesc(request.getAccountId())
                    .map(snapshotMapper::toResponseDTO)
                    .orElseThrow();
        }

        PortfolioSnapshot saved = snapshotRepository.save(snapshotMapper.toEntity(request, account));
        return snapshotMapper.toResponseDTO(saved);
    }

    //Helper
    private void verifyAccountOwnership(UUID accountId, UUID userId) {
        investmentAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found: " + accountId));
    }
}
