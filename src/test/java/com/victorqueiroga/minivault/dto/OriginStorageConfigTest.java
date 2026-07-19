package com.victorqueiroga.minivault.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OriginStorageConfigTest {

    @Test
    void shouldSetAndGetAllProperties() {
        OriginStorageConfig config = new OriginStorageConfig();

        config.setType("S3");
        config.setLocalPath("/data/storage");
        config.setEndpoint("http://minio:9000");
        config.setBucketName("backups");
        config.setRegion("us-east-1");
        config.setAccessKey("access123");
        config.setSecretKey("secret456");
        config.setPathStyleEnabled(true);

        assertEquals("S3", config.getType());
        assertEquals("/data/storage", config.getLocalPath());
        assertEquals("http://minio:9000", config.getEndpoint());
        assertEquals("backups", config.getBucketName());
        assertEquals("us-east-1", config.getRegion());
        assertEquals("access123", config.getAccessKey());
        assertEquals("secret456", config.getSecretKey());
        assertTrue(config.isPathStyleEnabled());
    }

    @Test
    void shouldDefaultPathStyleToFalse() {
        OriginStorageConfig config = new OriginStorageConfig();
        assertFalse(config.isPathStyleEnabled());
    }

    @Test
    void shouldAllowNullFields() {
        OriginStorageConfig config = new OriginStorageConfig();
        assertNull(config.getType());
        assertNull(config.getLocalPath());
        assertNull(config.getEndpoint());
        assertNull(config.getBucketName());
        assertNull(config.getRegion());
        assertNull(config.getAccessKey());
        assertNull(config.getSecretKey());
    }
}
