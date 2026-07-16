package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.OriginStorageConfig;
import com.victorqueiroga.minivault.exception.BackupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    public void uploadFile(OriginStorageConfig config, File file, String key) {
        S3Client client = buildClient(config);
        try {
            log.info("Uploading {} to S3 bucket {} with key {}", file.getName(), config.getBucketName(), key);
            client.putObject(PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build(), RequestBody.fromFile(file));
            log.info("Upload completed successfully");
        } catch (Exception e) {
            throw new BackupException("Failed to upload file to S3: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    public void downloadFile(OriginStorageConfig config, String key, Path destination) {
        S3Client client = buildClient(config);
        try {
            log.info("Downloading s3://{}/{} to {}", config.getBucketName(), key, destination);
            client.getObject(GetObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(key)
                    .build(), destination);
            log.info("Download completed successfully");
        } catch (Exception e) {
            throw new BackupException("Failed to download file from S3: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    public void deleteFile(OriginStorageConfig config, String key) {
        S3Client client = buildClient(config);
        try {
            log.info("Deleting s3://{}/{}", config.getBucketName(), key);
            client.deleteObject(b -> b.bucket(config.getBucketName()).key(key));
            log.info("Deletion completed successfully");
        } catch (Exception e) {
            throw new BackupException("Failed to delete file from S3: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    public List<String> listFiles(OriginStorageConfig config, String prefix) {
        S3Client client = buildClient(config);
        try {
            ListObjectsV2Response response = client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(config.getBucketName())
                    .prefix(prefix)
                    .build());
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BackupException("Failed to list files from S3: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    private S3Client buildClient(OriginStorageConfig config) {
        return S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .region(config.getRegion() != null ? Region.of(config.getRegion()) : Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(config.isPathStyleEnabled())
                        .build())
                .build();
    }
}
