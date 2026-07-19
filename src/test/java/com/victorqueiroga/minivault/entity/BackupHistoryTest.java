package com.victorqueiroga.minivault.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BackupHistoryTest {

    @Test
    void shouldCreateBackupHistoryWithDefaults() {
        BackupHistory history = new BackupHistory();
        assertNull(history.getId());
        assertNull(history.getBackupId());
        assertNull(history.getAction());
        assertNull(history.getStatus());
        assertNull(history.getMessage());
        assertNull(history.getCreatedAt());
    }

    @Test
    void shouldSetAndGetAllProperties() {
        BackupHistory history = new BackupHistory();
        LocalDateTime now = LocalDateTime.now();

        history.setId(1L);
        history.setBackupId(100L);
        history.setAction("INITIATED");
        history.setStatus(BackupStatus.PENDING);
        history.setMessage("Backup initiated");
        history.setCreatedAt(now);

        assertEquals(1L, history.getId());
        assertEquals(100L, history.getBackupId());
        assertEquals("INITIATED", history.getAction());
        assertEquals(BackupStatus.PENDING, history.getStatus());
        assertEquals("Backup initiated", history.getMessage());
        assertEquals(now, history.getCreatedAt());
    }

    @Test
    void prePersistShouldSetCreatedAt() {
        BackupHistory history = new BackupHistory();
        history.onCreate();
        assertNotNull(history.getCreatedAt());
    }

    @Test
    void shouldAcceptAllBackupStatusValues() {
        for (BackupStatus status : BackupStatus.values()) {
            BackupHistory history = new BackupHistory();
            history.setStatus(status);
            assertEquals(status, history.getStatus());
        }
    }
}
