package com.victorqueiroga.minivault.dto;

import com.victorqueiroga.minivault.entity.BackupStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BackupResponseTest {

    @Test
    void shouldSetAndGetAllProperties() {
        BackupResponse response = new BackupResponse();
        LocalDateTime now = LocalDateTime.now();

        response.setId(1L);
        response.setFilename("test.zip");
        response.setFileSize(512L);
        response.setStatus(BackupStatus.COMPLETED);
        response.setDatabaseType("mysql");
        response.setErrorMessage(null);
        response.setCreatedAt(now);
        response.setCompletedAt(now);

        assertEquals(1L, response.getId());
        assertEquals("test.zip", response.getFilename());
        assertEquals(512L, response.getFileSize());
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        assertEquals("mysql", response.getDatabaseType());
        assertNull(response.getErrorMessage());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getCompletedAt());
    }

    @Test
    void shouldAllowNullFields() {
        BackupResponse response = new BackupResponse();
        assertNull(response.getId());
        assertNull(response.getFilename());
        assertNull(response.getFileSize());
        assertNull(response.getStatus());
        assertNull(response.getDatabaseType());
        assertNull(response.getErrorMessage());
        assertNull(response.getCreatedAt());
        assertNull(response.getCompletedAt());
    }
}
