package com.victorqueiroga.minivault.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RestoreRequestTest {

    @Test
    void shouldSetAndGetBackupId() {
        RestoreRequest request = new RestoreRequest();
        request.setBackupId(42L);
        assertEquals(42L, request.getBackupId());
    }

    @Test
    void shouldSetAndGetDatabase() {
        RestoreRequest request = new RestoreRequest();
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setHost("localhost");
        request.setDatabase(creds);
        assertNotNull(request.getDatabase());
        assertEquals("localhost", request.getDatabase().getHost());
    }

    @Test
    void shouldSetAndGetOriginStorage() {
        RestoreRequest request = new RestoreRequest();
        OriginStorageConfig storage = new OriginStorageConfig();
        storage.setType("LOCAL");
        request.setOriginStorage(storage);
        assertNotNull(request.getOriginStorage());
        assertEquals("LOCAL", request.getOriginStorage().getType());
    }

    @Test
    void shouldAllowNullOriginStorage() {
        RestoreRequest request = new RestoreRequest();
        request.setBackupId(1L);
        request.setDatabase(new DatabaseCredentials());
        assertNull(request.getOriginStorage());
    }
}
