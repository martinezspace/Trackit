package com.trackit.analyticsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Base URLs are environment-specific — localhost in dev, Docker service name in compose
    @Value("${services.investment.base-url}")
    private String investmentServiceBaseUrl;

    @Value("${services.bank-account.base-url}")
    private String bankAccountServiceBaseUrl;

    @Bean
    public RestClient investmentRestClient() {
        return RestClient.builder()
                .baseUrl(investmentServiceBaseUrl)
                .build();
    }

    @Bean
    public RestClient bankAccountRestClient() {
        return RestClient.builder()
                .baseUrl(bankAccountServiceBaseUrl)
                .build();
    }
}