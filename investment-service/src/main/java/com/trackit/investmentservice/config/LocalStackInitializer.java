package com.trackit.investmentservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Component
//only runs when local profile is active - not in prod
@Profile("local")
public class LocalStackInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalStackInitializer.class);

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public LocalStackInitializer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void run(String... args) {
        createS3Bucket();
    }

    private void createS3Bucket() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            log.info("S3 bucket created: {}", bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            //Bucket already exists fine to ignore
            //Happens when container restarts but LocalStack volume perists
            log.info("S3 bucket already exists: {}", bucketName);
        }
    }
}
