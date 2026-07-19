package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.exception.BackupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipServiceTest {

    private final ZipService zipService = new ZipService();

    @Test
    void shouldCreateZipFromDirectory(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file1.txt"), "content1");
        Files.writeString(sourceDir.resolve("file2.txt"), "content2");

        Path subDir = sourceDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("nested.txt"), "nested");

        String zipPath = tempDir.resolve("output.zip").toString();

        File zipFile = zipService.createZip(sourceDir.toFile(), zipPath);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    void shouldExtractZipToDirectory(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("extract_test.txt"), "extract content");

        String zipPath = tempDir.resolve("archive.zip").toString();
        zipService.createZip(sourceDir.toFile(), zipPath);

        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        zipService.extractZip(zipPath, extractDir.toString());

        assertTrue(Files.exists(extractDir.resolve("extract_test.txt")));
        assertEquals("extract content", Files.readString(extractDir.resolve("extract_test.txt")));
    }

    @Test
    void shouldExtractZipWithSubdirectories(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir.resolve("nested/deep"));
        Files.writeString(sourceDir.resolve("nested/deep/deep_file.txt"), "deep");

        String zipPath = tempDir.resolve("archive.zip").toString();
        zipService.createZip(sourceDir.toFile(), zipPath);

        Path extractDir = tempDir.resolve("extracted");
        zipService.extractZip(zipPath, extractDir.toString());

        assertTrue(Files.exists(extractDir.resolve("nested/deep/deep_file.txt")));
    }

    @Test
    void shouldThrowExceptionForNonExistentSource() {
        File nonExistent = new File("/nonexistent/path");
        assertThrows(BackupException.class, () ->
                zipService.createZip(nonExistent, "/tmp/output.zip"));
    }

    @Test
    void shouldThrowExceptionForNonExistentZip() {
        assertThrows(BackupException.class, () ->
                zipService.extractZip("/nonexistent/file.zip", "/tmp/dest"));
    }

    @Test
    void shouldHandleEmptyDirectory(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("empty");
        Files.createDirectories(sourceDir);

        String zipPath = tempDir.resolve("empty.zip").toString();
        File zipFile = zipService.createZip(sourceDir.toFile(), zipPath);

        assertTrue(zipFile.exists());
    }
}
