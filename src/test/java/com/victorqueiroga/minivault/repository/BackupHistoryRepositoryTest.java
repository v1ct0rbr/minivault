package com.victorqueiroga.minivault.repository;

import com.victorqueiroga.minivault.BackupServiceApplication;
import com.victorqueiroga.minivault.entity.BackupHistory;
import com.victorqueiroga.minivault.entity.BackupStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BackupServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class BackupHistoryRepositoryTest {

    @Autowired
    private BackupHistoryRepository historyRepository;

    private BackupHistory createHistory(Long backupId, String action, BackupStatus status) {
        BackupHistory history = new BackupHistory();
        history.setBackupId(backupId);
        history.setAction(action);
        history.setStatus(status);
        history.setMessage(action + " message");
        return historyRepository.save(history);
    }

    @Test
    void shouldSaveAndFindHistory() {
        BackupHistory history = createHistory(1L, "INITIATED", BackupStatus.PENDING);

        assertNotNull(history.getId());
        assertEquals(1L, history.getBackupId());
    }

    @Test
    void shouldFindByBackupIdOrderByCreatedAtDesc() {
        createHistory(1L, "COMPLETED", BackupStatus.COMPLETED);
        createHistory(1L, "INITIATED", BackupStatus.PENDING);
        createHistory(1L, "DUMPING", BackupStatus.IN_PROGRESS);

        List<BackupHistory> histories = historyRepository.findByBackupIdOrderByCreatedAtDesc(1L);

        assertEquals(3, histories.size());
    }

    @Test
    void shouldReturnEmptyForNonExistentBackup() {
        List<BackupHistory> histories = historyRepository.findByBackupIdOrderByCreatedAtDesc(99L);
        assertTrue(histories.isEmpty());
    }

    @Test
    void shouldNotMixHistoryBetweenDifferentBackups() {
        createHistory(1L, "INITIATED", BackupStatus.PENDING);
        createHistory(2L, "COMPLETED", BackupStatus.COMPLETED);

        List<BackupHistory> histories = historyRepository.findByBackupIdOrderByCreatedAtDesc(1L);

        assertEquals(1, histories.size());
        assertEquals(1L, histories.get(0).getBackupId());
    }
}
