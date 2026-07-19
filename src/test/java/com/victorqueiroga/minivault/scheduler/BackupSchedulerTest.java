package com.victorqueiroga.minivault.scheduler;

import com.victorqueiroga.minivault.service.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock
    private BackupService backupService;

    private BackupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BackupScheduler(backupService);
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", true);
    }

    @Test
    void shouldRunScheduledBackup() {
        scheduler.runScheduledBackup();
        verify(backupService).executeScheduledBackup();
    }

    @Test
    void shouldNotRunWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", false);
        scheduler.runScheduledBackup();
        verify(backupService, never()).executeScheduledBackup();
    }

    @Test
    void shouldPropagateServiceException() {
        doThrow(new RuntimeException("Backup failed")).when(backupService).executeScheduledBackup();
        assertThrows(RuntimeException.class, () -> scheduler.runScheduledBackup());
        verify(backupService).executeScheduledBackup();
    }
}
