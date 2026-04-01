package com.trackit.investmentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${cloud.aws.region.static}")
    private String region;

    //Only set in local profile for localstack
    @Value("${cloud.aws.endpoint:#{null}}")
    private String endpoint;

    //Only set in local profile for localstack
    //on aws sdk uses defaultcredentialprovider
    @Value("${cloud.aws.credentials.access-key:#null}}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:#{null}}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        //Static credentials for LocalSStack
        if (accessKey != null && secretKey != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);
        }
        return builder.build();
    }
}
