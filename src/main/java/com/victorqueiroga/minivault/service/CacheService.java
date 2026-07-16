package com.victorqueiroga.minivault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @CacheEvict(value = "backups", allEntries = true)
    public void evictBackupCache() {
        log.debug("Backup cache evicted");
    }
}
