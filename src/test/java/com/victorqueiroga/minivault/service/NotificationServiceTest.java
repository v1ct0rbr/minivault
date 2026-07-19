package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.entity.Backup;
import com.victorqueiroga.minivault.entity.BackupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;

    private Backup createBackup() {
        Backup backup = new Backup();
        backup.setId(1L);
        backup.setFilename("backup_test.zip");
        backup.setFileSize(1024L);
        backup.setStatus(BackupStatus.COMPLETED);
        backup.setDatabaseType("postgresql");
        backup.setCompletedAt(LocalDateTime.now());
        return backup;
    }

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender);
        ReflectionTestUtils.setField(notificationService, "from", "test@localhost");
        ReflectionTestUtils.setField(notificationService, "to", "admin@localhost");
    }

    @Test
    void shouldNotSendCompletedNotificationWhenDisabled() {
        ReflectionTestUtils.setField(notificationService, "enabled", false);
        notificationService.notifyBackupCompleted(createBackup());
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldNotSendCompletedNotificationWhenSuccessDisabled() {
        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "notifyOnSuccess", false);
        notificationService.notifyBackupCompleted(createBackup());
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldSendCompletedNotification() {
        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "notifyOnSuccess", true);

        notificationService.notifyBackupCompleted(createBackup());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotSendFailedNotificationWhenDisabled() {
        ReflectionTestUtils.setField(notificationService, "enabled", false);
        notificationService.notifyBackupFailed(createBackup());
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldNotSendFailedNotificationWhenFailureDisabled() {
        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "notifyOnFailure", false);
        notificationService.notifyBackupFailed(createBackup());
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldSendFailedNotification() {
        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "notifyOnFailure", true);

        Backup backup = createBackup();
        backup.setStatus(BackupStatus.FAILED);
        backup.setErrorMessage("Something went wrong");
        notificationService.notifyBackupFailed(backup);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldHandleMailSenderExceptionGracefully() {
        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "notifyOnSuccess", true);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.notifyBackupCompleted(createBackup());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
