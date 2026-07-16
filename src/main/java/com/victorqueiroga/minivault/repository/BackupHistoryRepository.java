package com.victorqueiroga.minivault.repository;

import com.victorqueiroga.minivault.entity.BackupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
    List<BackupHistory> findByBackupIdOrderByCreatedAtDesc(Long backupId);
}
