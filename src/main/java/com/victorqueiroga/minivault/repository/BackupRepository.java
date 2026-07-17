package com.victorqueiroga.minivault.repository;

import com.victorqueiroga.minivault.entity.Backup;
import com.victorqueiroga.minivault.entity.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BackupRepository extends JpaRepository<Backup, Long> {
    Optional<Backup> findTopByStatusOrderByCompletedAtDesc(BackupStatus status);
}
