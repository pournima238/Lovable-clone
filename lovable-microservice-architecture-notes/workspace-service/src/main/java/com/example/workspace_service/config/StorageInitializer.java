// StorageInitializer.java - new file, handles bucket creation
package com.example.workspace_service.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageInitializer {

    private final MinioClient minioClient;

    @Value("${minio.bucket:projects}")
    private String projectBucket;

    @PostConstruct
    public void createBucketsIfNotExist() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(projectBucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(projectBucket).build()
                );
                log.info("Created MinIO bucket: {}", projectBucket);
            } else {
                log.info("MinIO bucket already exists: {}", projectBucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket {}: {}", projectBucket, e.getMessage(), e);
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }
}