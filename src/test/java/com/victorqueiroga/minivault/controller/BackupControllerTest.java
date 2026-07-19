package com.victorqueiroga.minivault.controller;

import com.victorqueiroga.minivault.dto.BackupRequest;
import com.victorqueiroga.minivault.dto.BackupResponse;
import com.victorqueiroga.minivault.dto.PageResponse;
import com.victorqueiroga.minivault.dto.RestoreRequest;
import com.victorqueiroga.minivault.entity.BackupStatus;
import com.victorqueiroga.minivault.service.BackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    @Mock
    private BackupService backupService;

    @InjectMocks
    private BackupController controller;

    @Test
    void createBackupShouldReturnOk() {
        BackupRequest request = new BackupRequest();
        BackupResponse response = new BackupResponse();
        response.setId(1L);
        response.setStatus(BackupStatus.COMPLETED);
        when(backupService.createBackup(request)).thenReturn(response);

        ResponseEntity<BackupResponse> result = controller.createBackup(request);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1L, result.getBody().getId());
        assertEquals(BackupStatus.COMPLETED, result.getBody().getStatus());
    }

    @Test
    void listBackupsShouldReturnPaginatedResponse() {
        PageResponse<BackupResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(backupService.listBackups(0, 10)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<BackupResponse>> result = controller.listBackups(0, 10);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }

    @Test
    void listBackupsShouldUseDefaultPagination() {
        PageResponse<BackupResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(backupService.listBackups(0, 10)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<BackupResponse>> result = controller.listBackups(0, 10);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void getBackupShouldReturnOk() {
        BackupResponse response = new BackupResponse();
        response.setId(1L);
        when(backupService.getBackup(1L)).thenReturn(response);

        ResponseEntity<BackupResponse> result = controller.getBackup(1L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void downloadBackupShouldReturnOctetStream() {
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "test_backup.zip");
        FileSystemResource resource = new FileSystemResource(tempFile);
        when(backupService.downloadBackup(1L)).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> result = controller.downloadBackup(1L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, result.getHeaders().getContentType());
        assertNotNull(result.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(result.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).contains("attachment"));
    }

    @Test
    void restoreBackupShouldReturnOk() {
        RestoreRequest request = new RestoreRequest();
        doNothing().when(backupService).restoreBackup(any(RestoreRequest.class));

        ResponseEntity<Void> result = controller.restoreBackup(1L, request);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1L, request.getBackupId());
        verify(backupService).restoreBackup(request);
    }

    @Test
    void deleteBackupShouldReturnNoContent() {
        doNothing().when(backupService).deleteBackup(1L);

        ResponseEntity<Void> result = controller.deleteBackup(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(backupService).deleteBackup(1L);
    }
}
