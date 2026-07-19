package com.victorqueiroga.minivault.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackupExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        BackupException ex = new BackupException("Backup failed");
        assertEquals("Backup failed", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        BackupException ex = new BackupException("Backup failed", cause);
        assertEquals("Backup failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void shouldBeRuntimeException() {
        BackupException ex = new BackupException("error");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
