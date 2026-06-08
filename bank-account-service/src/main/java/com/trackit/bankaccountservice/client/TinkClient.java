package com.trackit.bankaccountservice.client;

import com.trackit.bankaccountservice.config.TinkConfig;
import com.trackit.bankaccountservice.dto.tink.*;
import com.trackit.bankaccountservice.service.TinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TinkClient {

    //Scopes granted to the user token — covers all data we fetch per sync
    private static final String USER_SCOPES = "accounts:read,balances:read,transactions:read,credentials:read";

    private static final String TINK_LINK_BASE = "https://link.tink.com/1.0/transactions/connect-accounts";

    private final TinkConfig config;
    private final TinkTokenService tokenService;
    private final RestClient tinkRestClient;

    // --- Institution discovery ---

    public List<TinkProviderDTO> getProviders(String market) {
        return tinkRestClient.get()
                .uri("/api/v1/providers/{market}", market)
                .header("Authorization", "Bearer " + tokenService.getClientAccessToken())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<TinkProviderDTO>>() {});
    }

    // --- Connection setup ---

    //Creates a Tink user mapped to our userId — idempotent via external_user_id
    public TinkUserResponseDTO createUser(String externalUserId) {
        String body = """
                {"external_user_id":"%s","market":"PL","locale":"en_US"}
                """.formatted(externalUserId);

        return tinkRestClient.post()
                .uri("/api/v1/user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenService.getClientAccessToken())
                .body(body)
                .retrieve()
                .body(TinkUserResponseDTO.class);
    }

    //Obtains a short-lived authorization code used to build the Tink Link URL
    public TinkGrantResponseDTO createAuthorizationGrant(String tinkUserId) {
        String body = """
                {
                  "user_id":"%s",
                  "scope":"%s",
                  "redirect_uri":"%s",
                  "actor_client_id":"%s",
                  "id_hint":""
                }
                """.formatted(tinkUserId, USER_SCOPES, config.getRedirectUri(), config.getClientId());

        return tinkRestClient.post()
                .uri("/api/v1/oauth/authorization-grant/delegate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tokenService.getClientAccessToken())
                .body(body)
                .retrieve()
                .body(TinkGrantResponseDTO.class);
    }

    //Builds the URL the user must visit to authenticate with their bank
    public String buildLinkUrl(String authorizationCode) {
        return UriComponentsBuilder.fromUriString(TINK_LINK_BASE)
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("authorization_code", authorizationCode)
                .queryParam("market", "PL")
                .queryParam("locale", "en_US")
                .toUriString();
    }

    // --- Data fetching (requires user access token) ---

    public List<TinkAccountDTO> getAccounts(String userAccessToken) {
        TinkAccountListDTO response = tinkRestClient.get()
                .uri("/data/v2/accounts")
                .header("Authorization", "Bearer " + userAccessToken)
                .retrieve()
                .body(TinkAccountListDTO.class);
        return response != null && response.getAccounts() != null ? response.getAccounts() : List.of();
    }

    //Fetches all transaction pages for an account from a given date — handles cursor pagination
    public List<TinkTransactionDTO> getAllTransactions(String userAccessToken, String accountId, LocalDate from) {
        List<TinkTransactionDTO> all = new ArrayList<>();
        String pageToken = null;

        do {
            String uri = buildTransactionUri(accountId, from, pageToken);

            TinkTransactionListDTO page = tinkRestClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + userAccessToken)
                    .retrieve()
                    .body(TinkTransactionListDTO.class);

            if (page.getTransactions() != null) {
                all.addAll(page.getTransactions());
            }

            pageToken = page.getNextPageToken();
        } while (pageToken != null);

        return all;
    }

    private String buildTransactionUri(String accountId, LocalDate from, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/data/v2/transactions")
                .queryParam("accountIdIn", accountId)
                .queryParam("bookedDateGte", from.toString())
                .queryParam("pageSize", 100)
                .queryParam("statusIn", "BOOKED");

        if (pageToken != null) {
            builder.queryParam("pageToken", pageToken);
        }

        return builder.toUriString();
    }
}