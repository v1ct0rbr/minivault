package com.victorqueiroga.minivault.service;

import com.victorqueiroga.minivault.dto.DatabaseCredentials;
import com.victorqueiroga.minivault.exception.BackupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    public File dumpDatabase(DatabaseCredentials creds, String tempDir) {
        String type = creds.getDatabaseType() != null ? creds.getDatabaseType().toLowerCase() : "postgresql";
        String dumpFile = tempDir + File.separator + "database_dump.sql";
        List<String> command = buildDumpCommand(type, creds, dumpFile);

        log.info("Starting {} dump of database '{}' at {}:{}",
                type, creds.getDatabaseName(), creds.getHost(), creds.getPort());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        applyPasswordEnv(pb, type, creds.getPassword());

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                log.error("Dump failed with exit code {}: {}", exitCode, output);
                throw new BackupException("Database dump failed: " + output);
            }
            log.info("Database dump completed successfully");
            return new File(dumpFile);
        } catch (IOException | InterruptedException e) {
            throw new BackupException("Failed to execute database dump: " + e.getMessage(), e);
        }
    }

    public void restoreDatabase(DatabaseCredentials creds, String dumpFilePath) {
        String type = creds.getDatabaseType() != null ? creds.getDatabaseType().toLowerCase() : "postgresql";
        List<String> command = buildRestoreCommand(type, creds, dumpFilePath);

        log.info("Starting {} restore to database '{}' at {}:{}",
                type, creds.getDatabaseName(), creds.getHost(), creds.getPort());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        applyPasswordEnv(pb, type, creds.getPassword());

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                log.error("Restore failed with exit code {}: {}", exitCode, output);
                throw new BackupException("Database restore failed: " + output);
            }
            log.info("Database restore completed successfully");
        } catch (IOException | InterruptedException e) {
            throw new BackupException("Failed to execute database restore: " + e.getMessage(), e);
        }
    }

    private List<String> buildDumpCommand(String type, DatabaseCredentials creds, String outputFile) {
        List<String> cmd = new ArrayList<>();
        switch (type) {
            case "postgresql":
            case "postgres":
                cmd.add("pg_dump");
                cmd.add("-h");
                cmd.add(creds.getHost());
                cmd.add("-p");
                cmd.add(String.valueOf(creds.getPort()));
                cmd.add("-U");
                cmd.add(creds.getUsername());
                cmd.add("-Fc");
                cmd.add("-f");
                cmd.add(outputFile);
                cmd.add(creds.getDatabaseName());
                break;
            case "mysql":
                cmd.add("mysqldump");
                cmd.add("-h");
                cmd.add(creds.getHost());
                cmd.add("-P");
                cmd.add(String.valueOf(creds.getPort()));
                cmd.add("-u");
                cmd.add(creds.getUsername());
                cmd.add("-p" + creds.getPassword());
                cmd.add(creds.getDatabaseName());
                cmd.add("-r");
                cmd.add(outputFile);
                break;
            case "sqlserver":
            case "mssql":
                cmd.add("sqlcmd");
                cmd.add("-S");
                cmd.add(creds.getHost() + "," + creds.getPort());
                cmd.add("-U");
                cmd.add(creds.getUsername());
                cmd.add("-P");
                cmd.add(creds.getPassword());
                cmd.add("-d");
                cmd.add(creds.getDatabaseName());
                cmd.add("-Q");
                cmd.add("BACKUP DATABASE [" + creds.getDatabaseName() + "] TO DISK='" + outputFile + "'");
                break;
            default:
                throw new BackupException("Unsupported database type: " + type);
        }
        return cmd;
    }

    private List<String> buildRestoreCommand(String type, DatabaseCredentials creds, String inputFile) {
        List<String> cmd = new ArrayList<>();
        switch (type) {
            case "postgresql":
            case "postgres":
                cmd.add("pg_restore");
                cmd.add("-h");
                cmd.add(creds.getHost());
                cmd.add("-p");
                cmd.add(String.valueOf(creds.getPort()));
                cmd.add("-U");
                cmd.add(creds.getUsername());
                cmd.add("-d");
                cmd.add(creds.getDatabaseName());
                cmd.add("--clean");
                cmd.add("--if-exists");
                cmd.add(inputFile);
                break;
            case "mysql":
                cmd.add("mysql");
                cmd.add("-h");
                cmd.add(creds.getHost());
                cmd.add("-P");
                cmd.add(String.valueOf(creds.getPort()));
                cmd.add("-u");
                cmd.add(creds.getUsername());
                cmd.add("-p" + creds.getPassword());
                cmd.add(creds.getDatabaseName());
                cmd.add("<");
                cmd.add(inputFile);
                break;
            case "sqlserver":
            case "mssql":
                cmd.add("sqlcmd");
                cmd.add("-S");
                cmd.add(creds.getHost() + "," + creds.getPort());
                cmd.add("-U");
                cmd.add(creds.getUsername());
                cmd.add("-P");
                cmd.add(creds.getPassword());
                cmd.add("-Q");
                cmd.add("RESTORE DATABASE [" + creds.getDatabaseName() + "] FROM DISK='" + inputFile + "'");
                break;
            default:
                throw new BackupException("Unsupported database type: " + type);
        }
        return cmd;
    }

    private void applyPasswordEnv(ProcessBuilder pb, String type, String password) {
        if ((type.equals("postgresql") || type.equals("postgres"))) {
            pb.environment().put("PGPASSWORD", password);
        }
    }
}
