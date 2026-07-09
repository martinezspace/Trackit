package com.trackit.analyticsservice.client;

import com.trackit.analyticsservice.dto.client.bank.BankAccountDTO;
import com.trackit.analyticsservice.dto.client.bank.BankTransactionDTO;
import com.trackit.analyticsservice.dto.client.bank.PagedResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BankAccountServiceClient {

    // Field name matches bean name — Spring resolves the correct RestClient without @Qualifier
    private final RestClient bankAccountRestClient;

    public List<BankAccountDTO> getBankAccounts(UUID userId) {
        return bankAccountRestClient.get()
                .uri("/api/bank-accounts")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // Fetches all transaction pages for a date range — bank-account-service caps at 200 per page
    public List<BankTransactionDTO> getTransactions(UUID userId, LocalDate from, LocalDate to) {
        List<BankTransactionDTO> all = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last) {
            int currentPage = page;
            PagedResponseDTO<BankTransactionDTO> response = bankAccountRestClient.get()
                    .uri(u -> u.path("/api/transactions")
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("size", 200)
                            .queryParam("page", currentPage)
                            .build())
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.getContent() == null) break;
            all.addAll(response.getContent());
            last = response.isLast();
            page++;
        }

        return all;
    }
}