package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.entity.Backup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${notification.enabled:false}")
    private boolean enabled;

    @Value("${notification.from}")
    private String from;

    @Value("${notification.to}")
    private String to;

    @Value("${notification.on-success:false}")
    private boolean notifyOnSuccess;

    @Value("${notification.on-failure:true}")
    private boolean notifyOnFailure;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyBackupCompleted(Backup backup) {
        if (!enabled) {
            log.debug("Notifications are disabled");
            return;
        }
        if (!notifyOnSuccess) {
            log.debug("Notification on success is disabled");
            return;
        }
        String subject = "Minivault - Backup completed: " + backup.getFilename();
        String body = "Backup completed successfully.\n\n"
                + "ID: " + backup.getId() + "\n"
                + "File: " + backup.getFilename() + "\n"
                + "Size: " + backup.getFileSize() + " bytes\n"
                + "Database: " + backup.getDatabaseType() + "\n"
                + "Completed at: " + backup.getCompletedAt();
        send(subject, body);
    }

    public void notifyBackupFailed(Backup backup) {
        if (!enabled) {
            log.debug("Notifications are disabled");
            return;
        }
        if (!notifyOnFailure) {
            log.debug("Notification on failure is disabled");
            return;
        }
        String subject = "Minivault - Backup FAILED: " + backup.getFilename();
        String body = "Backup failed.\n\n"
                + "ID: " + backup.getId() + "\n"
                + "File: " + backup.getFilename() + "\n"
                + "Database: " + backup.getDatabaseType() + "\n"
                + "Error: " + backup.getErrorMessage() + "\n"
                + "Failed at: " + backup.getCompletedAt();
        send(subject, body);
    }

    private void send(String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Notification email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }
}
