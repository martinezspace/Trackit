package com.trackit.bankaccountservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final TinkConfig tinkConfig;

    @Bean
    public RestClient tinkRestClient() {
        return RestClient.builder()
                .baseUrl(tinkConfig.getBaseUrl())
                .build();
    }
}