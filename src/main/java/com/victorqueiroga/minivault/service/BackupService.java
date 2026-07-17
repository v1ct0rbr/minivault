package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.*;
import com.victorqueiroga.minivault.entity.Backup;
import com.victorqueiroga.minivault.entity.BackupHistory;
import com.victorqueiroga.minivault.entity.BackupStatus;
import com.victorqueiroga.minivault.exception.BackupException;
import com.victorqueiroga.minivault.repository.BackupHistoryRepository;
import com.victorqueiroga.minivault.repository.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String SNAPSHOTS_DIR_NAME = "__snapshots__";

    private final BackupRepository backupRepository;
    private final BackupHistoryRepository historyRepository;
    private final DatabaseService databaseService;
    private final ZipService zipService;
    private final S3StorageService s3StorageService;
    private final CacheService cacheService;
    private final NotificationService notificationService;

    @Value("${backup.output.dir}")
    private String outputDir;

    @Value("${backup.output.type:LOCAL}")
    private String outputType;

    @Value("${backup.temp.dir}")
    private String tempDir;

    @Value("${backup.storage.snapshots.dir:}")
    private String storageSnapshotsDir;

    public BackupService(BackupRepository backupRepository,
                         BackupHistoryRepository historyRepository,
                         DatabaseService databaseService,
                         ZipService zipService,
                         S3StorageService s3StorageService,
                         CacheService cacheService,
                         NotificationService notificationService) {
        this.backupRepository = backupRepository;
        this.historyRepository = historyRepository;
        this.databaseService = databaseService;
        this.zipService = zipService;
        this.s3StorageService = s3StorageService;
        this.cacheService = cacheService;
        this.notificationService = notificationService;
    }

    @CacheEvict(value = "backups", allEntries = true)
    public BackupResponse createBackup(BackupRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String type = request.getDatabase().getDatabaseType() != null
                ? request.getDatabase().getDatabaseType() : "postgresql";
        String filename = "backup_" + type + "_" + timestamp + ".zip";
        String workDir = tempDir + File.separator + timestamp;

        Backup backup = new Backup();
        backup.setFilename(filename);
        backup.setDatabaseType(type);
        backup.setStatus(BackupStatus.PENDING);
        backup = backupRepository.save(backup);

        addHistory(backup.getId(), "INITIATED", BackupStatus.PENDING, "Backup process initiated");

        try {
            backup.setStatus(BackupStatus.IN_PROGRESS);
            backup = backupRepository.save(backup);

            Files.createDirectories(Path.of(workDir));

            addHistory(backup.getId(), "DUMPING", BackupStatus.IN_PROGRESS, "Starting database dump");
            databaseService.dumpDatabase(request.getDatabase(), workDir);
            log.info("Database dump completed");

            if (request.getOriginStorage() != null) {
                addHistory(backup.getId(), "STORAGE", BackupStatus.IN_PROGRESS,
                        "Collecting origin storage files");
                LocalDateTime lastBackupTime = backupRepository
                        .findTopByStatusOrderByCompletedAtDesc(BackupStatus.COMPLETED)
                        .map(Backup::getCompletedAt)
                        .orElse(null);
                collectOriginStorage(request.getOriginStorage(), workDir, backup.getId(), lastBackupTime);
            }

            addHistory(backup.getId(), "COMPRESSING", BackupStatus.IN_PROGRESS, "Creating ZIP archive");
            String localZipPath = outputDir + File.separator + filename;
            File zipFile = zipService.createZip(new File(workDir), localZipPath);

            boolean isS3Output = "S3".equalsIgnoreCase(outputType);
            String finalPath = localZipPath;

            if (isS3Output) {
                addHistory(backup.getId(), "UPLOADING", BackupStatus.IN_PROGRESS, "Uploading to S3 output");
                OriginStorageConfig s3Output = buildS3OutputConfig();
                s3StorageService.uploadFile(s3Output, zipFile, filename);
                finalPath = "s3://" + s3Output.getBucketName() + "/" + filename;
                cleanupLocalFile(localZipPath);
            }

            backup.setFileSize(zipFile.length());
            backup.setFilePath(finalPath);
            backup.setStatus(BackupStatus.COMPLETED);
            backup.setCompletedAt(LocalDateTime.now());
            backup = backupRepository.save(backup);

            addHistory(backup.getId(), "COMPLETED", BackupStatus.COMPLETED, "Backup completed successfully");
            notificationService.notifyBackupCompleted(backup);

            cleanupTemp(workDir);
            log.info("Backup {} completed successfully: {}", backup.getId(), filename);
            return toResponse(backup);

        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            cleanupSnapshotDir(backup.getId());
            backup.setStatus(BackupStatus.FAILED);
            backup.setErrorMessage(e.getMessage());
            backup.setCompletedAt(LocalDateTime.now());
            backupRepository.save(backup);
            addHistory(backup.getId(), "FAILED", BackupStatus.FAILED, "Backup failed: " + e.getMessage());
            notificationService.notifyBackupFailed(backup);
            cleanupTemp(workDir);
            throw new BackupException("Backup failed: " + e.getMessage(), e);
        }
    }

    private void collectOriginStorage(OriginStorageConfig config, String workDir,
                                       Long backupId, LocalDateTime lastBackupTime) throws IOException {
        String type = config.getType() != null ? config.getType().toUpperCase() : "S3";
        Path storageDir = Path.of(workDir, "storage");
        Files.createDirectories(storageDir);

        if ("LOCAL".equals(type)) {
            String localPath = config.getLocalPath();
            if (localPath == null || localPath.isBlank()) {
                throw new BackupException("LOCAL origin storage requires localPath");
            }
            File source = new File(localPath);
            if (!source.exists()) {
                throw new BackupException("Local storage path does not exist: " + localPath);
            }

            String snapshotsBase = getSnapshotsBaseDir();
            Path snapshotDir = Path.of(snapshotsBase, String.valueOf(backupId));
            Path prevSnapshot = findPreviousSnapshotDir(snapshotsBase, backupId);

            Files.createDirectories(snapshotDir);

            log.info("Syncing local storage from {} to persistent snapshot {} (previous: {})",
                    localPath, snapshotDir, prevSnapshot != null ? prevSnapshot : "(none)");
            try {
                rsyncWithLinkDest(localPath, snapshotDir.toString(), prevSnapshot);
            } catch (Exception e) {
                log.warn("Rsync failed, falling back to full copy: {}", e.getMessage());
                copyDirectory(source, snapshotDir.toFile());
            }

            log.info("Creating hardlinks from snapshot {} to workdir {}", snapshotDir, storageDir);
            try {
                hardlinkDirectory(snapshotDir, storageDir);
            } catch (Exception e) {
                log.warn("Hardlink failed, falling back to file copy: {}", e.getMessage());
                copyDirectory(snapshotDir.toFile(), storageDir.toFile());
            }

        } else {
            Instant after = lastBackupTime != null
                    ? lastBackupTime.atZone(ZoneId.systemDefault()).toInstant()
                    : Instant.EPOCH;
            log.info("Downloading S3 storage files modified after {} from bucket {}",
                    after, config.getBucketName());
            var files = s3StorageService.listFilesAfter(config, "", after);
            if (files.isEmpty()) {
                log.info("No new or modified files found since last backup");
                return;
            }
            for (String key : files) {
                Path dest = storageDir.resolve(key.replace("/", "_"));
                s3StorageService.downloadFile(config, key, dest);
            }
            log.info("Downloaded {} new/modified files from S3 storage", files.size());
        }
    }

    private void rsyncWithLinkDest(String source, String destination, Path linkDest)
            throws IOException, InterruptedException {
        String src = source.endsWith(File.separator) ? source : source + File.separator;
        String dest = destination.endsWith(File.separator) ? destination : destination + File.separator;

        List<String> command = new ArrayList<>();
        command.add("rsync");
        command.add("-a");
        command.add("--delete");
        if (linkDest != null) {
            command.add("--link-dest=" + linkDest.toString());
        }
        command.add(src);
        command.add(dest);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new BackupException("rsync failed with exit code " + exitCode + ": " + output);
        }
    }

    private String getSnapshotsBaseDir() {
        if (storageSnapshotsDir != null && !storageSnapshotsDir.isBlank()) {
            return storageSnapshotsDir;
        }
        if ("LOCAL".equalsIgnoreCase(outputType)) {
            return outputDir + File.separator + SNAPSHOTS_DIR_NAME;
        }
        return tempDir + File.separator + SNAPSHOTS_DIR_NAME;
    }

    private Path findPreviousSnapshotDir(String snapshotsBase, Long currentBackupId) {
        File base = new File(snapshotsBase);
        if (!base.exists()) return null;

        File[] dirs = base.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) return null;

        Long prevId = null;
        for (File dir : dirs) {
            try {
                Long id = Long.parseLong(dir.getName());
                if (id < currentBackupId && (prevId == null || id > prevId)) {
                    prevId = id;
                }
            } catch (NumberFormatException ignored) {}
        }
        return prevId != null ? Path.of(snapshotsBase, String.valueOf(prevId)) : null;
    }

    private void hardlinkDirectory(Path sourceDir, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        try (Stream<Path> files = Files.list(sourceDir)) {
            for (Path file : files.toList()) {
                Path dest = destDir.resolve(file.getFileName());
                if (Files.isDirectory(file)) {
                    hardlinkDirectory(file, dest);
                } else {
                    Files.createLink(dest, file);
                }
            }
        }
    }

    private void cleanupSnapshotDir(Long backupId) {
        String snapshotsBase = getSnapshotsBaseDir();
        Path snapshotDir = Path.of(snapshotsBase, String.valueOf(backupId));
        try {
            if (Files.exists(snapshotDir)) {
                try (Stream<Path> files = Files.walk(snapshotDir)) {
                    files.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                log.info("Cleaned up snapshot dir for failed backup: {}", snapshotDir);
            }
        } catch (Exception e) {
            log.warn("Could not clean up snapshot dir: {}", snapshotDir, e);
        }
    }

    private OriginStorageConfig buildS3OutputConfig() {
        OriginStorageConfig cfg = new OriginStorageConfig();
        cfg.setType("S3");
        cfg.setEndpoint(System.getenv("OUTPUT_STORAGE_ENDPOINT"));
        cfg.setBucketName(System.getenv("OUTPUT_STORAGE_BUCKET"));
        cfg.setAccessKey(System.getenv("OUTPUT_STORAGE_ACCESS_KEY"));
        cfg.setSecretKey(System.getenv("OUTPUT_STORAGE_SECRET_KEY"));
        cfg.setRegion(System.getenv("OUTPUT_STORAGE_REGION"));
        cfg.setPathStyleEnabled(Boolean.parseBoolean(System.getenv("OUTPUT_STORAGE_PATH_STYLE")));
        return cfg;
    }

    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();
        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                copyDirectory(file, new File(destDir, file.getName()));
            } else {
                Files.copy(file.toPath(), new File(destDir, file.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Cacheable(value = "backups", key = "#page + '-' + #size")
    public PageResponse<BackupResponse> listBackups(int page, int size) {
        Page<Backup> backupPage = backupRepository.findAll(PageRequest.of(page, size));
        return new PageResponse<>(
                backupPage.getContent().stream().map(this::toResponse).toList(),
                backupPage.getNumber(),
                backupPage.getSize(),
                backupPage.getTotalElements(),
                backupPage.getTotalPages()
        );
    }

    public BackupResponse getBackup(Long id) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new BackupException("Backup not found: " + id));
        return toResponse(backup);
    }

    public Resource downloadBackup(Long id) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new BackupException("Backup not found: " + id));

        if (backup.getFilePath() != null && backup.getFilePath().startsWith("s3://")) {
            throw new BackupException("Backup is stored on S3, use the storage console to download");
        }

        File file = new File(backup.getFilePath());
        if (!file.exists()) {
            throw new BackupException("Backup file not found on disk: " + backup.getFilePath());
        }
        return new FileSystemResource(file);
    }

    @CacheEvict(value = "backups", allEntries = true)
    public void restoreBackup(RestoreRequest request) {
        Backup backup = backupRepository.findById(request.getBackupId())
                .orElseThrow(() -> new BackupException("Backup not found: " + request.getBackupId()));

        backup.setStatus(BackupStatus.RESTORING);
        backupRepository.save(backup);
        addHistory(backup.getId(), "RESTORING", BackupStatus.RESTORING, "Starting restore process");

        String restoreDir = tempDir + File.separator + "restore_" + backup.getId();
        String zipPath = backup.getFilePath();
        boolean tempZip = false;

        try {
            Files.createDirectories(Path.of(restoreDir));

            if (zipPath != null && zipPath.startsWith("s3://")) {
                addHistory(backup.getId(), "DOWNLOADING", BackupStatus.RESTORING,
                        "Downloading backup from S3");
                zipPath = restoreDir + File.separator + backup.getFilename();
                OriginStorageConfig s3Config = buildS3OutputConfig();
                s3StorageService.downloadFile(s3Config, backup.getFilename(), Path.of(zipPath));
                tempZip = true;
            }

            addHistory(backup.getId(), "EXTRACTING", BackupStatus.RESTORING, "Extracting backup archive");
            zipService.extractZip(zipPath, restoreDir);

            File dumpFile = Path.of(restoreDir, "database_dump.sql").toFile();
            if (dumpFile.exists()) {
                addHistory(backup.getId(), "RESTORING_DB", BackupStatus.RESTORING,
                        "Restoring database");
                databaseService.restoreDatabase(request.getDatabase(), dumpFile.getAbsolutePath());
            } else {
                log.warn("No database dump found in backup archive, skipping database restore");
            }

            Path storageDir = Path.of(restoreDir, "storage");
            if (Files.exists(storageDir) && request.getOriginStorage() != null
                    && "LOCAL".equalsIgnoreCase(request.getOriginStorage().getType())) {
                String destPath = request.getOriginStorage().getLocalPath();
                if (destPath != null && !destPath.isBlank()) {
                    addHistory(backup.getId(), "RESTORING_FILES", BackupStatus.RESTORING,
                            "Restoring storage files to " + destPath);
                    File dest = new File(destPath);
                    copyDirectory(storageDir.toFile(), dest);
                    log.info("Storage files restored to {}", destPath);
                }
            }

            backup.setStatus(BackupStatus.RESTORED);
            backupRepository.save(backup);
            addHistory(backup.getId(), "RESTORED", BackupStatus.RESTORED,
                    "Restore completed successfully");

        } catch (Exception e) {
            log.error("Restore failed: {}", e.getMessage(), e);
            backup.setStatus(BackupStatus.RESTORE_FAILED);
            backup.setErrorMessage(e.getMessage());
            backupRepository.save(backup);
            addHistory(backup.getId(), "RESTORE_FAILED", BackupStatus.RESTORE_FAILED,
                    "Restore failed: " + e.getMessage());
            throw new BackupException("Restore failed: " + e.getMessage(), e);
        } finally {
            cleanupTemp(restoreDir);
            if (tempZip) {
                cleanupLocalFile(zipPath);
            }
        }
    }

    @CacheEvict(value = "backups", allEntries = true)
    public void deleteBackup(Long id) {
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new BackupException("Backup not found: " + id));

        String path = backup.getFilePath();
        if (path != null && !path.startsWith("s3://")) {
            File file = new File(path);
            if (file.exists() && !file.delete()) {
                log.warn("Could not delete file: {}", path);
            }
        }

        cleanupSnapshotDir(id);

        backupRepository.delete(backup);
        addHistory(id, "DELETED", BackupStatus.COMPLETED, "Backup deleted");
        log.info("Backup {} deleted successfully", id);
    }

    public void executeScheduledBackup() {
        log.info("Starting scheduled automatic backup");
        BackupRequest request = new BackupRequest();
        DatabaseCredentials dbCreds = new DatabaseCredentials();
        dbCreds.setDatabaseType(System.getenv("SCHEDULED_BACKUP_DB_TYPE"));
        dbCreds.setHost(System.getenv("SCHEDULED_BACKUP_DB_HOST"));
        String portStr = System.getenv("SCHEDULED_BACKUP_DB_PORT");
        dbCreds.setPort(portStr != null ? Integer.parseInt(portStr) : 5432);
        dbCreds.setDatabaseName(System.getenv("SCHEDULED_BACKUP_DB_NAME"));
        dbCreds.setUsername(System.getenv("SCHEDULED_BACKUP_DB_USER"));
        dbCreds.setPassword(System.getenv("SCHEDULED_BACKUP_DB_PASSWORD"));
        request.setDatabase(dbCreds);

        String originType = System.getenv("ORIGIN_STORAGE_TYPE");
        if (originType != null && !originType.isBlank()) {
            OriginStorageConfig origin = new OriginStorageConfig();
            origin.setType(originType.toUpperCase());
            if ("LOCAL".equalsIgnoreCase(originType)) {
                origin.setLocalPath(System.getenv("ORIGIN_STORAGE_LOCAL_PATH"));
            } else {
                origin.setEndpoint(System.getenv("ORIGIN_STORAGE_ENDPOINT"));
                origin.setBucketName(System.getenv("ORIGIN_STORAGE_BUCKET"));
                origin.setAccessKey(System.getenv("ORIGIN_STORAGE_ACCESS_KEY"));
                origin.setSecretKey(System.getenv("ORIGIN_STORAGE_SECRET_KEY"));
                origin.setRegion(System.getenv("ORIGIN_STORAGE_REGION"));
                origin.setPathStyleEnabled(Boolean.parseBoolean(
                        System.getenv("ORIGIN_STORAGE_PATH_STYLE")));
            }
            request.setOriginStorage(origin);
        }

        try {
            createBackup(request);
        } catch (Exception e) {
            log.error("Scheduled backup failed: {}", e.getMessage(), e);
        }
    }

    private void addHistory(Long backupId, String action, BackupStatus status, String message) {
        BackupHistory history = new BackupHistory();
        history.setBackupId(backupId);
        history.setAction(action);
        history.setStatus(status);
        history.setMessage(message);
        historyRepository.save(history);
    }

    private void cleanupLocalFile(String path) {
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (Exception e) {
            log.warn("Could not delete local file after S3 upload: {}", path);
        }
    }

    private void cleanupTemp(String path) {
        try {
            File dir = new File(path);
            if (dir.exists()) {
                for (File f : dir.listFiles()) f.delete();
                dir.delete();
            }
        } catch (Exception e) {
            log.warn("Could not clean up temp dir: {}", path);
        }
    }

    private BackupResponse toResponse(Backup backup) {
        BackupResponse resp = new BackupResponse();
        resp.setId(backup.getId());
        resp.setFilename(backup.getFilename());
        resp.setFileSize(backup.getFileSize());
        resp.setStatus(backup.getStatus());
        resp.setDatabaseType(backup.getDatabaseType());
        resp.setErrorMessage(backup.getErrorMessage());
        resp.setCreatedAt(backup.getCreatedAt());
        resp.setCompletedAt(backup.getCompletedAt());
        return resp;
    }
}
