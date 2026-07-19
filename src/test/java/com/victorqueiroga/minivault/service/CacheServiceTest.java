package com.victorqueiroga.minivault.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Test
    void evictBackupCacheShouldNotThrow() {
        CacheService cacheService = new CacheService();
        cacheService.evictBackupCache();
    }
}
