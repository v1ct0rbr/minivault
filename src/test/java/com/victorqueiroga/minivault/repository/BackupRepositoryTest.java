package com.victorqueiroga.minivault.repository;

import com.victorqueiroga.minivault.BackupServiceApplication;
import com.victorqueiroga.minivault.entity.Backup;
import com.victorqueiroga.minivault.entity.BackupStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BackupServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class BackupRepositoryTest {

    @Autowired
    private BackupRepository backupRepository;

    private Backup createBackup(String filename, BackupStatus status, LocalDateTime completedAt) {
        Backup backup = new Backup();
        backup.setFilename(filename);
        backup.setFileSize(1024L);
        backup.setFilePath("/tmp/" + filename);
        backup.setStatus(status);
        backup.setDatabaseType("postgresql");
        backup.setCompletedAt(completedAt);
        return backupRepository.save(backup);
    }

    @Test
    void shouldSaveAndFindBackup() {
        Backup backup = createBackup("test_backup.zip", BackupStatus.COMPLETED, LocalDateTime.now());

        Optional<Backup> found = backupRepository.findById(backup.getId());

        assertTrue(found.isPresent());
        assertEquals("test_backup.zip", found.get().getFilename());
        assertEquals(BackupStatus.COMPLETED, found.get().getStatus());
    }

    @Test
    void shouldFindTopByStatusOrderByCompletedAtDesc() {
        createBackup("old.zip", BackupStatus.COMPLETED, LocalDateTime.now().minusHours(2));
        createBackup("new.zip", BackupStatus.COMPLETED, LocalDateTime.now());
        createBackup("pending.zip", BackupStatus.PENDING, null);

        Optional<Backup> result = backupRepository.findTopByStatusOrderByCompletedAtDesc(BackupStatus.COMPLETED);

        assertTrue(result.isPresent());
        assertEquals("new.zip", result.get().getFilename());
    }

    @Test
    void shouldReturnEmptyWhenNoBackupWithStatus() {
        createBackup("completed.zip", BackupStatus.COMPLETED, LocalDateTime.now());

        Optional<Backup> result = backupRepository.findTopByStatusOrderByCompletedAtDesc(BackupStatus.FAILED);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldDeleteBackup() {
        Backup backup = createBackup("delete_me.zip", BackupStatus.COMPLETED, LocalDateTime.now());

        backupRepository.delete(backup);

        assertFalse(backupRepository.findById(backup.getId()).isPresent());
    }

    @Test
    void shouldEnforceUniqueFilename() {
        createBackup("unique.zip", BackupStatus.COMPLETED, LocalDateTime.now());

        Backup duplicate = new Backup();
        duplicate.setFilename("unique.zip");
        duplicate.setFileSize(512L);
        duplicate.setFilePath("/tmp/unique.zip");
        duplicate.setStatus(BackupStatus.PENDING);
        duplicate.setDatabaseType("mysql");

        assertThrows(Exception.class, () -> backupRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldReturnAllBackups() {
        createBackup("a.zip", BackupStatus.COMPLETED, LocalDateTime.now());
        createBackup("b.zip", BackupStatus.FAILED, LocalDateTime.now());
        createBackup("c.zip", BackupStatus.PENDING, null);

        assertEquals(3, backupRepository.count());
    }
}
