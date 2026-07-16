package com.victorqueiroga.minivault.repository;

import com.victorqueiroga.minivault.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupRepository extends JpaRepository<Backup, Long> {
}
