package com.victorqueiroga.minivault.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BackupTest {

    @Test
    void shouldCreateBackupWithDefaults() {
        Backup backup = new Backup();
        assertNull(backup.getId());
        assertNull(backup.getFilename());
        assertNull(backup.getFileSize());
        assertNull(backup.getFilePath());
        assertNull(backup.getStatus());
        assertNull(backup.getDatabaseType());
        assertNull(backup.getErrorMessage());
        assertNull(backup.getCreatedAt());
        assertNull(backup.getCompletedAt());
    }

    @Test
    void shouldSetAndGetAllProperties() {
        Backup backup = new Backup();
        LocalDateTime now = LocalDateTime.now();

        backup.setId(1L);
        backup.setFilename("backup_test.zip");
        backup.setFileSize(1024L);
        backup.setFilePath("/tmp/backups/backup_test.zip");
        backup.setStatus(BackupStatus.COMPLETED);
        backup.setDatabaseType("postgresql");
        backup.setErrorMessage("error");
        backup.setCreatedAt(now);
        backup.setCompletedAt(now);

        assertEquals(1L, backup.getId());
        assertEquals("backup_test.zip", backup.getFilename());
        assertEquals(1024L, backup.getFileSize());
        assertEquals("/tmp/backups/backup_test.zip", backup.getFilePath());
        assertEquals(BackupStatus.COMPLETED, backup.getStatus());
        assertEquals("postgresql", backup.getDatabaseType());
        assertEquals("error", backup.getErrorMessage());
        assertEquals(now, backup.getCreatedAt());
        assertEquals(now, backup.getCompletedAt());
    }

    @Test
    void prePersistShouldSetCreatedAt() {
        Backup backup = new Backup();
        backup.onCreate();
        assertNotNull(backup.getCreatedAt());
    }
}
