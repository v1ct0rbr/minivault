package com.victorqueiroga.minivault.dto;

import com.victorqueiroga.minivault.entity.BackupStatus;
import java.time.LocalDateTime;

public class BackupResponse {

    private Long id;
    private String filename;
    private Long fileSize;
    private BackupStatus status;
    private String databaseType;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
