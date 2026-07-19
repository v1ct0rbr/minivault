package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.DatabaseCredentials;
import com.victorqueiroga.minivault.exception.BackupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseServiceTest {

    private final DatabaseService databaseService = new DatabaseService();

    private DatabaseCredentials createCreds(String type) {
        DatabaseCredentials creds = new DatabaseCredentials();
        creds.setHost("localhost");
        creds.setPort(5432);
        creds.setDatabaseName("testdb");
        creds.setUsername("testuser");
        creds.setPassword("testpass");
        creds.setDatabaseType(type);
        return creds;
    }

    @Test
    void shouldDumpPostgresqlDatabase(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("postgresql");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(0);
                    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mock.start()).thenReturn(process);
                })) {

            File result = databaseService.dumpDatabase(creds, tempDir.toString());

            assertNotNull(result);
            assertTrue(result.getName().startsWith("database_dump"));
        }
    }

    @Test
    void shouldDumpMysqlDatabase(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("mysql");
        creds.setPort(3306);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(0);
                    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mock.start()).thenReturn(process);
                })) {

            File result = databaseService.dumpDatabase(creds, tempDir.toString());
            assertNotNull(result);
        }
    }

    @Test
    void shouldDumpSqlServerDatabase(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("sqlserver");
        creds.setPort(1433);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(0);
                    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mock.start()).thenReturn(process);
                })) {

            File result = databaseService.dumpDatabase(creds, tempDir.toString());
            assertNotNull(result);
        }
    }

    @Test
    void shouldThrowExceptionForUnsupportedDatabaseType() {
        DatabaseCredentials creds = createCreds("unsupported");

        assertThrows(BackupException.class, () ->
                databaseService.dumpDatabase(creds, "/tmp"));
    }

    @Test
    void shouldDefaultToPostgresqlWhenTypeIsNull(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds(null);

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(0);
                    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mock.start()).thenReturn(process);
                })) {

            File result = databaseService.dumpDatabase(creds, tempDir.toString());
            assertNotNull(result);
        }
    }

    @Test
    void shouldThrowExceptionWhenDumpFails(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("postgresql");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(1);
                    when(process.getInputStream()).thenReturn(
                            new ByteArrayInputStream("ERROR: connection failed".getBytes()));
                    when(mock.start()).thenReturn(process);
                })) {

            assertThrows(BackupException.class, () ->
                    databaseService.dumpDatabase(creds, tempDir.toString()));
        }
    }

    @Test
    void shouldThrowExceptionWhenDumpProcessInterrupted(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("postgresql");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenThrow(new InterruptedException("interrupted"));
                    when(mock.start()).thenReturn(process);
                })) {

            assertThrows(BackupException.class, () ->
                    databaseService.dumpDatabase(creds, tempDir.toString()));
        }
    }

    @Test
    void shouldRestorePostgresqlDatabase(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("postgresql");
        String dumpFile = tempDir.resolve("dump.sql").toString();
        Files.writeString(Path.of(dumpFile), "SQL content");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(0);
                    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mock.start()).thenReturn(process);
                })) {

            databaseService.restoreDatabase(creds, dumpFile);

            ProcessBuilder captured = mocked.constructed().get(0);
            assertNotNull(captured);
        }
    }

    @Test
    void shouldThrowExceptionWhenRestoreFails(@TempDir Path tempDir) throws Exception {
        DatabaseCredentials creds = createCreds("postgresql");
        String dumpFile = tempDir.resolve("dump.sql").toString();
        Files.writeString(Path.of(dumpFile), "SQL content");

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process process = mock(Process.class);
                    when(process.waitFor()).thenReturn(1);
                    when(process.getInputStream()).thenReturn(
                            new ByteArrayInputStream("ERROR: restore failed".getBytes()));
                    when(mock.start()).thenReturn(process);
                })) {

            assertThrows(BackupException.class, () ->
                    databaseService.restoreDatabase(creds, dumpFile));
        }
    }

    @Test
    void shouldThrowExceptionForUnsupportedRestoreType() {
        DatabaseCredentials creds = createCreds("unsupported");
        assertThrows(BackupException.class, () ->
                databaseService.restoreDatabase(creds, "/tmp/dump.sql"));
    }
}
