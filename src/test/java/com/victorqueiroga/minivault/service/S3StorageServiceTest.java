package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.OriginStorageConfig;
import com.victorqueiroga.minivault.exception.BackupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private final S3StorageService s3StorageService = new S3StorageService();

    private OriginStorageConfig createConfig() {
        OriginStorageConfig config = new OriginStorageConfig();
        config.setEndpoint("http://localhost:9000");
        config.setBucketName("test-bucket");
        config.setAccessKey("access");
        config.setSecretKey("secret");
        config.setRegion("us-east-1");
        config.setPathStyleEnabled(true);
        return config;
    }

    private S3ClientBuilder mockBuilder() {
        S3ClientBuilder builder = mock(S3ClientBuilder.class);
        when(builder.endpointOverride(any(URI.class))).thenReturn(builder);
        when(builder.region(any(Region.class))).thenReturn(builder);
        when(builder.credentialsProvider(any(StaticCredentialsProvider.class))).thenReturn(builder);
        when(builder.serviceConfiguration(any(S3Configuration.class))).thenReturn(builder);
        return builder;
    }

    @Test
    void shouldUploadFile(@TempDir Path tempDir) {
        OriginStorageConfig config = createConfig();
        File file = tempDir.resolve("test.txt").toFile();
        try {
            Files.writeString(file.toPath(), "test content");
        } catch (Exception e) {
            fail("Failed to create test file");
        }

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);

            s3StorageService.uploadFile(config, file, "test-key");

            verify(mockClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldThrowExceptionOnUploadFailure(@TempDir Path tempDir) {
        OriginStorageConfig config = createConfig();
        File file = tempDir.resolve("test.txt").toFile();

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);
            when(mockClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(new RuntimeException("Upload failed"));

            assertThrows(BackupException.class, () ->
                    s3StorageService.uploadFile(config, file, "test-key"));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldDownloadFile(@TempDir Path tempDir) {
        OriginStorageConfig config = createConfig();
        Path destination = tempDir.resolve("downloaded.txt");

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);

            s3StorageService.downloadFile(config, "test-key", destination);

            verify(mockClient).getObject(any(GetObjectRequest.class), eq(destination));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldDeleteFile() {
        OriginStorageConfig config = createConfig();

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);

            s3StorageService.deleteFile(config, "test-key");

            verify(mockClient).deleteObject(any(java.util.function.Consumer.class));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldListFiles() {
        OriginStorageConfig config = createConfig();

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);

            ListObjectsV2Response mockResponse = mock(ListObjectsV2Response.class);
            S3Object s3Object = S3Object.builder().key("file1.txt").build();
            when(mockClient.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);
            when(mockResponse.contents()).thenReturn(List.of(s3Object));

            List<String> files = s3StorageService.listFiles(config, "prefix/");

            assertEquals(1, files.size());
            assertEquals("file1.txt", files.get(0));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldListFilesAfterDate() {
        OriginStorageConfig config = createConfig();
        Instant after = Instant.now().minusSeconds(3600);

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);

            ListObjectsV2Response mockResponse = mock(ListObjectsV2Response.class);
            S3Object oldFile = S3Object.builder().key("old.txt")
                    .lastModified(Instant.now().minusSeconds(7200)).build();
            S3Object newFile = S3Object.builder().key("new.txt")
                    .lastModified(Instant.now()).build();
            when(mockClient.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);
            when(mockResponse.contents()).thenReturn(List.of(oldFile, newFile));

            List<String> files = s3StorageService.listFilesAfter(config, "", after);

            assertEquals(1, files.size());
            assertEquals("new.txt", files.get(0));
            verify(mockClient).close();
        }
    }

    @Test
    void shouldThrowExceptionOnListFilesFailure() {
        OriginStorageConfig config = createConfig();

        try (var s3Static = mockStatic(S3Client.class)) {
            S3ClientBuilder builder = mockBuilder();
            S3Client mockClient = mock(S3Client.class);
            when(builder.build()).thenReturn(mockClient);
            s3Static.when(S3Client::builder).thenReturn(builder);
            when(mockClient.listObjectsV2(any(ListObjectsV2Request.class)))
                    .thenThrow(new RuntimeException("List failed"));

            assertThrows(BackupException.class, () ->
                    s3StorageService.listFiles(config, "prefix/"));
            verify(mockClient).close();
        }
    }
}
