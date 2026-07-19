package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.*;
import com.victorqueiroga.minivault.entity.Backup;
import com.victorqueiroga.minivault.entity.BackupHistory;
import com.victorqueiroga.minivault.entity.BackupStatus;
import com.victorqueiroga.minivault.exception.BackupException;
import com.victorqueiroga.minivault.repository.BackupHistoryRepository;
import com.victorqueiroga.minivault.repository.BackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock private BackupRepository backupRepository;
    @Mock private BackupHistoryRepository historyRepository;
    @Mock private DatabaseService databaseService;
    @Mock private ZipService zipService;
    @Mock private S3StorageService s3StorageService;
    @Mock private CacheService cacheService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BackupService backupService;

    @TempDir
    Path tempDir;

    private BackupRequest createBackupRequest() {
        BackupRequest request = new BackupRequest();
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setHost("localhost");
        creds.setPort(5432);
        creds.setDatabaseName("testdb");
        creds.setUsername("testuser");
        creds.setPassword("testpass");
        creds.setDatabaseType("postgresql");
        request.setDatabase(creds);
        return request;
    }

    private Backup createBackupEntity(Long id, String filename, BackupStatus status) {
        Backup backup = new Backup();
        backup.setId(id);
        backup.setFilename(filename);
        backup.setFileSize(1024L);
        backup.setFilePath(tempDir.resolve("backups").resolve(filename).toString());
        backup.setStatus(status);
        backup.setDatabaseType("postgresql");
        backup.setCreatedAt(LocalDateTime.now());
        backup.setCompletedAt(LocalDateTime.now());
        return backup;
    }

    @BeforeEach
    void setUp() {
        String outputDir = tempDir.resolve("backups").toString();
        String tmpDir = tempDir.resolve("temp").toString();
        ReflectionTestUtils.setField(backupService, "outputDir", outputDir);
        ReflectionTestUtils.setField(backupService, "outputType", "LOCAL");
        ReflectionTestUtils.setField(backupService, "tempDir", tmpDir);
        ReflectionTestUtils.setField(backupService, "storageSnapshotsDir", "");
    }

    @Test
    void createBackupShouldSucceedWithLocalOutput() {
        BackupRequest request = createBackupRequest();
        Backup savedBackup = createBackupEntity(1L, "backup_postgresql_20240101_120000.zip", BackupStatus.PENDING);

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });

        BackupResponse response = backupService.createBackup(request);

        assertNotNull(response);
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        verify(backupRepository, atLeast(3)).save(any(Backup.class));
        verify(historyRepository, atLeast(4)).save(any(BackupHistory.class));
        verify(databaseService).dumpDatabase(any(DatabaseCredentials.class), anyString());
        verify(zipService).createZip(any(File.class), anyString());
        verify(notificationService).notifyBackupCompleted(any(Backup.class));
    }

    @Test
    void createBackupShouldSucceedWithS3Output() {
        BackupRequest request = createBackupRequest();
        ReflectionTestUtils.setField(backupService, "outputType", "S3");

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(2L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });

        BackupResponse response = backupService.createBackup(request);

        assertNotNull(response);
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        verify(s3StorageService).uploadFile(any(OriginStorageConfig.class), any(File.class), anyString());
        verify(notificationService).notifyBackupCompleted(any(Backup.class));
    }

    @Test
    void createBackupShouldHandleFailure() {
        BackupRequest request = createBackupRequest();

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(3L);
            return b;
        });
        doThrow(new RuntimeException("DB connection failed"))
                .when(databaseService).dumpDatabase(any(DatabaseCredentials.class), anyString());

        assertThrows(BackupException.class, () -> backupService.createBackup(request));

        verify(notificationService).notifyBackupFailed(any(Backup.class));
        verify(backupRepository, atLeast(2)).save(any(Backup.class));
    }

    @Test
    void createBackupShouldHandleOriginStorageLocal() {
        BackupRequest request = createBackupRequest();
        OriginStorageConfig origin = new OriginStorageConfig();
        origin.setType("LOCAL");
        origin.setLocalPath(tempDir.resolve("origin-storage").toString());
        request.setOriginStorage(origin);

        File originDir = tempDir.resolve("origin-storage").toFile();
        originDir.mkdirs();

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(4L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });

        BackupResponse response = backupService.createBackup(request);

        assertNotNull(response);
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        verify(notificationService).notifyBackupCompleted(any(Backup.class));
    }

    @Test
    void createBackupShouldHandleOriginStorageS3() {
        BackupRequest request = createBackupRequest();
        OriginStorageConfig origin = new OriginStorageConfig();
        origin.setType("S3");
        origin.setEndpoint("http://minio:9000");
        origin.setBucketName("origin-bucket");
        origin.setAccessKey("access");
        origin.setSecretKey("secret");
        origin.setRegion("us-east-1");
        request.setOriginStorage(origin);

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(5L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });
        when(s3StorageService.listFilesAfter(any(OriginStorageConfig.class), anyString(), any()))
                .thenReturn(List.of());

        BackupResponse response = backupService.createBackup(request);

        assertNotNull(response);
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        verify(s3StorageService).listFilesAfter(any(OriginStorageConfig.class), anyString(), any());
    }

    @Test
    void createBackupShouldDefaultTypeToPostgresqlWhenNull() {
        BackupRequest request = createBackupRequest();
        request.getDatabase().setDatabaseType(null);

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(6L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });

        BackupResponse response = backupService.createBackup(request);

        assertNotNull(response);
        assertEquals("postgresql", response.getDatabaseType());
    }

    @Test
    void listBackupsShouldReturnPaginatedResponse() {
        Page<Backup> page = new PageImpl<>(
                List.of(createBackupEntity(1L, "test.zip", BackupStatus.COMPLETED)),
                PageRequest.of(0, 10), 1);

        when(backupRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PageResponse<BackupResponse> response = backupService.listBackups(0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void listBackupsShouldReturnEmptyPage() {
        Page<Backup> page = new PageImpl<>(List.of());
        when(backupRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PageResponse<BackupResponse> response = backupService.listBackups(0, 10);

        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void getBackupShouldReturnBackupResponse() {
        Backup backup = createBackupEntity(1L, "test.zip", BackupStatus.COMPLETED);
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        BackupResponse response = backupService.getBackup(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test.zip", response.getFilename());
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
    }

    @Test
    void getBackupShouldThrowWhenNotFound() {
        when(backupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BackupException.class, () -> backupService.getBackup(99L));
    }

    @Test
    void downloadBackupShouldReturnFileSystemResource() throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path filePath = backupDir.resolve("download_test.zip");
        Files.writeString(filePath, "backup content");

        Backup backup = createBackupEntity(1L, "download_test.zip", BackupStatus.COMPLETED);
        backup.setFilePath(filePath.toString());
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        Resource resource = backupService.downloadBackup(1L);

        assertNotNull(resource);
        assertInstanceOf(FileSystemResource.class, resource);
        assertTrue(resource.exists());
    }

    @Test
    void downloadBackupShouldThrowWhenS3Stored() {
        Backup backup = createBackupEntity(1L, "s3_backup.zip", BackupStatus.COMPLETED);
        backup.setFilePath("s3://bucket/backup.zip");
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        assertThrows(BackupException.class, () -> backupService.downloadBackup(1L));
    }

    @Test
    void downloadBackupShouldThrowWhenFileNotFound() {
        Backup backup = createBackupEntity(1L, "missing.zip", BackupStatus.COMPLETED);
        backup.setFilePath("/nonexistent/path/backup.zip");
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        assertThrows(BackupException.class, () -> backupService.downloadBackup(1L));
    }

    @Test
    void restoreBackupShouldSucceedWithLocalBackup() throws Exception {
        Backup backup = createBackupEntity(1L, "restore_test.zip", BackupStatus.COMPLETED);
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));
        when(backupRepository.save(any(Backup.class))).thenReturn(backup);
        doAnswer(invocation -> {
            String destDir = invocation.getArgument(1);
            Files.createDirectories(Path.of(destDir));
            Files.writeString(Path.of(destDir, "database_dump.sql"), "SQL content");
            return null;
        }).when(zipService).extractZip(anyString(), anyString());

        RestoreRequest request = new RestoreRequest();
        request.setBackupId(1L);
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setHost("localhost");
        creds.setPort(5432);
        creds.setDatabaseName("testdb");
        creds.setUsername("testuser");
        creds.setPassword("testpass");
        creds.setDatabaseType("postgresql");
        request.setDatabase(creds);

        backupService.restoreBackup(request);

        verify(databaseService).restoreDatabase(any(DatabaseCredentials.class), anyString());
        verify(backupRepository, atLeast(2)).save(any(Backup.class));
    }

    @Test
    void restoreBackupShouldThrowWhenBackupNotFound() {
        when(backupRepository.findById(99L)).thenReturn(Optional.empty());

        RestoreRequest request = new RestoreRequest();
        request.setBackupId(99L);
        request.setDatabase(new DatabaseCredentials());

        assertThrows(BackupException.class, () -> backupService.restoreBackup(request));
    }

    @Test
    void deleteBackupShouldDeleteLocalFileAndRecord() throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);
        Path filePath = backupDir.resolve("delete_test.zip");
        Files.writeString(filePath, "to be deleted");

        Backup backup = createBackupEntity(1L, "delete_test.zip", BackupStatus.COMPLETED);
        backup.setFilePath(filePath.toString());
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        backupService.deleteBackup(1L);

        verify(backupRepository).delete(backup);
        verify(historyRepository).save(any(BackupHistory.class));
        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteBackupShouldSkipFileDeletionForS3Backup() {
        Backup backup = createBackupEntity(1L, "s3_backup.zip", BackupStatus.COMPLETED);
        backup.setFilePath("s3://bucket/backup.zip");
        when(backupRepository.findById(1L)).thenReturn(Optional.of(backup));

        backupService.deleteBackup(1L);

        verify(backupRepository).delete(backup);
    }

    @Test
    void deleteBackupShouldThrowWhenNotFound() {
        when(backupRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BackupException.class, () -> backupService.deleteBackup(99L));
    }

    @Test
    void executeScheduledBackupShouldBuildRequestFromEnv() {
        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });
        when(zipService.createZip(any(File.class), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(1);
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
            return f;
        });

        backupService.executeScheduledBackup();
        verify(backupRepository, atLeastOnce()).save(any(Backup.class));
    }

    @Test
    void executeScheduledBackupShouldLogException() {
        when(backupRepository.save(any(Backup.class))).thenThrow(new RuntimeException("error"));

        backupService.executeScheduledBackup();
        verify(backupRepository, atLeastOnce()).save(any(Backup.class));
    }
}
