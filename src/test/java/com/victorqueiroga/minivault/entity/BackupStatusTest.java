package com.victorqueiroga.minivault.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackupStatusTest {

    @Test
    void shouldHaveAllExpectedValues() {
        assertEquals(7, BackupStatus.values().length);
        assertEquals(BackupStatus.PENDING, BackupStatus.valueOf("PENDING"));
        assertEquals(BackupStatus.IN_PROGRESS, BackupStatus.valueOf("IN_PROGRESS"));
        assertEquals(BackupStatus.COMPLETED, BackupStatus.valueOf("COMPLETED"));
        assertEquals(BackupStatus.FAILED, BackupStatus.valueOf("FAILED"));
        assertEquals(BackupStatus.RESTORING, BackupStatus.valueOf("RESTORING"));
        assertEquals(BackupStatus.RESTORED, BackupStatus.valueOf("RESTORED"));
        assertEquals(BackupStatus.RESTORE_FAILED, BackupStatus.valueOf("RESTORE_FAILED"));
    }

    @Test
    void shouldMaintainOrder() {
        BackupStatus[] values = BackupStatus.values();
        assertEquals(BackupStatus.PENDING, values[0]);
        assertEquals(BackupStatus.IN_PROGRESS, values[1]);
        assertEquals(BackupStatus.COMPLETED, values[2]);
        assertEquals(BackupStatus.FAILED, values[3]);
        assertEquals(BackupStatus.RESTORING, values[4]);
        assertEquals(BackupStatus.RESTORED, values[5]);
        assertEquals(BackupStatus.RESTORE_FAILED, values[6]);
    }
}
