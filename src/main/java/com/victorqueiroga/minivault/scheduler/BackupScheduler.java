package com.victorqueiroga.minivault.scheduler;

import com.victorqueiroga.minivault.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;

    @Value("${backup.schedule.enabled:true}")
    private boolean scheduleEnabled;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Scheduled(cron = "${backup.schedule.cron:0 0 2 * * ?}")
    public void runScheduledBackup() {
        if (!scheduleEnabled) {
            log.debug("Scheduled backup is disabled");
            return;
        }
        log.info("Triggering scheduled backup");
        backupService.executeScheduledBackup();
    }
}
