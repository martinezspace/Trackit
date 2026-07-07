package com.trackit.analyticsservice.client;

import com.trackit.analyticsservice.dto.client.bank.BankAccountDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}