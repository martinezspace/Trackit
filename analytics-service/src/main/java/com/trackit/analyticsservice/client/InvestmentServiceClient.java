package com.trackit.analyticsservice.client;

import com.trackit.analyticsservice.dto.client.investment.HoldingDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentTransactionDTO;
import com.trackit.analyticsservice.dto.client.investment.PortfolioSnapshotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvestmentServiceClient {

    // Field name matches bean name — Spring resolves the correct RestClient without @Qualifier
    private final RestClient investmentRestClient;

    public List<InvestmentAccountDTO> getInvestmentAccounts(UUID userId) {
        return investmentRestClient.get()
                .uri("/api/investment-accounts")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PortfolioSnapshotDTO getLatestPortfolioSnapshot(UUID accountId, UUID userId) {
        return investmentRestClient.get()
                .uri("/api/portfolio-snapshots/latest?accountId={accountId}", accountId)
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(PortfolioSnapshotDTO.class);
    }

    public List<HoldingDTO> getHoldings(UUID accountId, UUID userId) {
        return investmentRestClient.get()
                .uri("/api/holdings?accountId={accountId}", accountId)
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<InvestmentTransactionDTO> getInvestmentTransactions(UUID accountId, UUID userId) {
        return investmentRestClient.get()
                .uri("/api/transactions?accountId={accountId}", accountId)
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}