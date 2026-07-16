package com.victorqueiroga.minivault.controller;

import com.victorqueiroga.minivault.dto.BackupRequest;
import com.victorqueiroga.minivault.dto.BackupResponse;
import com.victorqueiroga.minivault.dto.PageResponse;
import com.victorqueiroga.minivault.dto.RestoreRequest;
import com.victorqueiroga.minivault.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backups")
@Tag(name = "Backups", description = "Gerenciamento de backups")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    @Operation(summary = "Criar um novo backup")
    @ApiResponse(responseCode = "200", description = "Backup criado com sucesso")
    public ResponseEntity<BackupResponse> createBackup(@Valid @RequestBody BackupRequest request) {
        BackupResponse response = backupService.createBackup(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar backups com paginacao")
    @ApiResponse(responseCode = "200", description = "Lista paginada de backups")
    public ResponseEntity<PageResponse<BackupResponse>> listBackups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BackupResponse> response = backupService.listBackups(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de um backup")
    @ApiResponse(responseCode = "200", description = "Detalhes do backup")
    public ResponseEntity<BackupResponse> getBackup(@PathVariable Long id) {
        BackupResponse response = backupService.getBackup(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Baixar arquivo de backup")
    @ApiResponse(responseCode = "200", description = "Arquivo de backup")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long id) {
        Resource resource = backupService.downloadBackup(id);
        String filename = resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restaurar um backup")
    @ApiResponse(responseCode = "200", description = "Restauracao iniciada com sucesso")
    public ResponseEntity<Void> restoreBackup(@PathVariable Long id,
                                              @Valid @RequestBody RestoreRequest request) {
        request.setBackupId(id);
        backupService.restoreBackup(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir um backup")
    @ApiResponse(responseCode = "204", description = "Backup excluido com sucesso")
    public ResponseEntity<Void> deleteBackup(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return ResponseEntity.noContent().build();
    }
}
