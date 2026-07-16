package com.victorqueiroga.minivault.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RestoreRequest {

    @NotNull
    private Long backupId;

    @NotNull
    @Valid
    private DatabaseCredentials database;

    @Valid
    private OriginStorageConfig originStorage;

    public Long getBackupId() { return backupId; }
    public void setBackupId(Long backupId) { this.backupId = backupId; }

    public DatabaseCredentials getDatabase() { return database; }
    public void setDatabase(DatabaseCredentials database) { this.database = database; }

    public OriginStorageConfig getOriginStorage() { return originStorage; }
    public void setOriginStorage(OriginStorageConfig originStorage) { this.originStorage = originStorage; }
}
