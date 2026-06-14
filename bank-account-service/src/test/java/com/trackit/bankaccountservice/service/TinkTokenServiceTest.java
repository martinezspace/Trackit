package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.config.TinkConfig;
import com.trackit.bankaccountservice.model.TinkToken;
import com.trackit.bankaccountservice.repository.TinkTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class TinkTokenServiceTest {

    @Mock
    private TinkTokenRepository tokenRepository;

    private MockRestServiceServer mockServer;
    private TinkTokenService tokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        TinkConfig config = new TinkConfig();
        config.setClientId("client-id");
        config.setClientSecret("client-secret");

        // MockRestServiceServer replaces the HTTP transport — cleanest way to test RestClient
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        tokenService = new TinkTokenService(tokenRepository, config, builder.build());
    }

    // getClientAccessToken

    @Test
    public void getClientAccessToken_returnsCachedToken_whenNotExpired() {
        TinkToken stored = new TinkToken();
        stored.setAccessToken("cached-token");
        // Expires well beyond the 5-min safety margin
        stored.setAccessExpiresAt(LocalDateTime.now().plusHours(1));
        when(tokenRepository.findFirstBy()).thenReturn(Optional.of(stored));

        String result = tokenService.getClientAccessToken();

        assertThat(result).isEqualTo("cached-token");
        mockServer.verify(); // Asserts no HTTP call was made
    }

    @Test
    public void getClientAccessToken_fetchesNewToken_whenNoCachedTokenExists() {
        when(tokenRepository.findFirstBy()).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubHttpResponse("new-token", 3600);

        String result = tokenService.getClientAccessToken();

        assertThat(result).isEqualTo("new-token");
    }

    @Test
    public void getClientAccessToken_fetchesNewToken_whenTokenAboutToExpire() {
        TinkToken expiring = new TinkToken();
        expiring.setAccessToken("expiring-token");
        // Within the 5-min safety margin — must be treated as expired
        expiring.setAccessExpiresAt(LocalDateTime.now().plusMinutes(3));
        when(tokenRepository.findFirstBy()).thenReturn(Optional.of(expiring));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubHttpResponse("fresh-token", 3600);

        String result = tokenService.getClientAccessToken();

        assertThat(result).isEqualTo("fresh-token");
    }

    @Test
    public void getClientAccessToken_fetchesNewToken_whenTokenAlreadyExpired() {
        TinkToken expired = new TinkToken();
        expired.setAccessToken("expired-token");
        expired.setAccessExpiresAt(LocalDateTime.now().minusMinutes(10));
        when(tokenRepository.findFirstBy()).thenReturn(Optional.of(expired));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubHttpResponse("refreshed-token", 3600);

        String result = tokenService.getClientAccessToken();

        assertThat(result).isEqualTo("refreshed-token");
    }

    @Test
    public void getClientAccessToken_upsertsExistingRow_whenRefreshing() {
        TinkToken existing = new TinkToken();
        existing.setAccessToken("old-token");
        existing.setAccessExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findFirstBy()).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubHttpResponse("updated-token", 3600);

        tokenService.getClientAccessToken();

        // Must update the existing row, not insert a new entity
        verify(tokenRepository).save(existing);
    }

    @Test
    public void getClientAccessToken_createsNewRow_whenNoneExists() {
        when(tokenRepository.findFirstBy()).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubHttpResponse("brand-new-token", 3600);

        tokenService.getClientAccessToken();

        verify(tokenRepository).save(any(TinkToken.class));
    }

    // getUserAccessToken

    @Test
    public void getUserAccessToken_exchangesCodeAndReturnsToken() {
        stubHttpResponse("user-access-token", 300);

        String result = tokenService.getUserAccessToken("auth-code-123");

        assertThat(result).isEqualTo("user-access-token");
    }

    @Test
    public void getUserAccessToken_doesNotPersistToken() {
        stubHttpResponse("user-token", 300);

        tokenService.getUserAccessToken("auth-code-123");

        verify(tokenRepository, never()).save(any());
    }

    // Helpers

    private void stubHttpResponse(String accessToken, int expiresIn) {
        String json = """
                {"access_token":"%s","expires_in":%d}""".formatted(accessToken, expiresIn);
        mockServer.expect(requestTo("/api/v1/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}