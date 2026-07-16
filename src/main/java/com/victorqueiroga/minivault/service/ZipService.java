package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.exception.BackupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    private static final Logger log = LoggerFactory.getLogger(ZipService.class);

    public File createZip(File sourceDir, String outputPath) {
        log.info("Creating ZIP archive from {} to {}", sourceDir.getAbsolutePath(), outputPath);
        File zipFile = new File(outputPath);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            addToZip(sourceDir, sourceDir, zos);
            log.info("ZIP archive created successfully: {}", zipFile.getAbsolutePath());
            return zipFile;
        } catch (IOException e) {
            throw new BackupException("Failed to create ZIP archive: " + e.getMessage(), e);
        }
    }

    private void addToZip(File baseDir, File dir, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                addToZip(baseDir, file, zos);
            } else {
                String entryName = baseDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }
}
