package com.victorqueiroga.minivault.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class BackupRequest {

    @NotNull
    @Valid
    private DatabaseCredentials database;

    @Valid
    private OriginStorageConfig originStorage;

    public DatabaseCredentials getDatabase() { return database; }
    public void setDatabase(DatabaseCredentials database) { this.database = database; }

    public OriginStorageConfig getOriginStorage() { return originStorage; }
    public void setOriginStorage(OriginStorageConfig originStorage) { this.originStorage = originStorage; }
}
