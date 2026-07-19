package com.victorqueiroga.minivault.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackupRequestTest {

    @Test
    void shouldSetAndGetDatabase() {
        BackupRequest request = new BackupRequest();
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setHost("localhost");
        request.setDatabase(creds);
        assertNotNull(request.getDatabase());
        assertEquals("localhost", request.getDatabase().getHost());
    }

    @Test
    void shouldSetAndGetOriginStorage() {
        BackupRequest request = new BackupRequest();
        OriginStorageConfig storage = new OriginStorageConfig();
        storage.setType("S3");
        request.setOriginStorage(storage);
        assertNotNull(request.getOriginStorage());
        assertEquals("S3", request.getOriginStorage().getType());
    }

    @Test
    void shouldAllowNullOriginStorage() {
        BackupRequest request = new BackupRequest();
        request.setDatabase(new DatabaseCredentials());
        assertNull(request.getOriginStorage());
    }
}
