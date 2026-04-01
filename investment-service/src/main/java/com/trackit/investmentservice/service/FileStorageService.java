package com.trackit.investmentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    //Upload a CSV file to S3
    //Returns the s3Key - the file path inside the bucket
    // e.g. imports/user-uuid/batch-uuid/filename.csv
    public String uploadFile(MultipartFile file, UUID userId, UUID batchId) {
        String s3Key = buildS3Key(userId, batchId, file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            log.info("File uploaded to S3: {}", s3Key);
            return s3Key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + s3Key, e);
        }
    }

    //Download a file from S3 as an InputStream
    //Used by CSV parsers to read the file content
    public InputStream downloadFile(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
        log.info("File downloaded from S3: {}", s3Key);
        return response;
    }

    //Delete a file from S3 - called when a batch is cancelled
    public void deleteFile(String s3Key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
        log.info("File deleted from S3: {}", s3Key);
    }

    //helper
    //Build structured S3 key so files are organised by user and batch
    //imports/123e4567/456e7890/january.csv
    private String buildS3Key(UUID userId, UUID batchId, String filename) {
        return String.format("import/%s/%s/%s", userId, batchId, filename);
    }
}
