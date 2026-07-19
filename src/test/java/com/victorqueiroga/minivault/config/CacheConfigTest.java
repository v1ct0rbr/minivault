package com.victorqueiroga.minivault.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    @Test
    void shouldCreateCaffeineCacheManager() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();

        assertNotNull(manager);
        assertInstanceOf(CaffeineCacheManager.class, manager);
        assertTrue(manager.getCacheNames().contains("backups"));
    }
}
