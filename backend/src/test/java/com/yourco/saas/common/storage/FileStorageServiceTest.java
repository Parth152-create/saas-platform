package com.yourco.saas.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    private StorageProperties properties;
    private InMemoryFileStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setMaxFileSizeBytes(1024 * 1024); // 1 MB for testing
        storageService = new InMemoryFileStorageService(properties);
    }

    @Test
    void storeAndRetrieveFileSuccessfully() {
        byte[] content = "Sample PDF Content".getBytes(StandardCharsets.UTF_8);
        StoredFileMetadata meta = storageService.store("tenant-1", "report.pdf", "application/pdf", content);

        assertNotNull(meta.storageKey());
        assertEquals("report.pdf", meta.sanitizedFilename());
        assertEquals("application/pdf", meta.contentType());
        assertEquals(content.length, meta.sizeBytes());
        assertEquals("tenant-1", meta.tenantId());

        byte[] retrieved = storageService.retrieve("tenant-1", meta.storageKey());
        assertArrayEquals(content, retrieved);
        assertTrue(storageService.exists("tenant-1", meta.storageKey()));
    }

    @Test
    void tenantIsolationPreventsCrossTenantAccess() {
        byte[] content = "Confidential Tenant A Document".getBytes(StandardCharsets.UTF_8);
        StoredFileMetadata metaA = storageService.store("tenant-a", "secrets.pdf", "application/pdf", content);

        // Tenant B attempts to retrieve Tenant A's storageKey
        assertThrows(StorageException.class, () ->
                storageService.retrieve("tenant-b", metaA.storageKey()),
                "Tenant B must not be able to retrieve Tenant A's document");

        assertFalse(storageService.exists("tenant-b", metaA.storageKey()),
                "Tenant A's file must not be visible to Tenant B");
    }

    @Test
    void fileSizeExceededThrowsException() {
        byte[] largeContent = new byte[(int) properties.getMaxFileSizeBytes() + 10];

        assertThrows(FileSizeExceededException.class, () ->
                storageService.store("tenant-1", "large.pdf", "application/pdf", largeContent));
    }

    @Test
    void disallowedMimeTypeThrowsInvalidFileException() {
        byte[] content = "echo evil".getBytes(StandardCharsets.UTF_8);

        assertThrows(InvalidFileException.class, () ->
                storageService.store("tenant-1", "script.sh", "application/x-sh", content));
    }

    @Test
    void filenameSanitizationNeutralizesPathTraversal() {
        assertEquals("secret.pdf", FileValidationUtils.sanitizeFilename("../../../secret.pdf"));
        assertEquals("passwd", FileValidationUtils.sanitizeFilename("..\\..\\etc\\passwd"));
        assertEquals("normal_file.pdf", FileValidationUtils.sanitizeFilename("normal file.pdf"));
    }

    @Test
    void deleteRemovesFileFromTenant() {
        byte[] content = "Temporary Content".getBytes(StandardCharsets.UTF_8);
        StoredFileMetadata meta = storageService.store("tenant-1", "temp.pdf", "application/pdf", content);

        assertTrue(storageService.exists("tenant-1", meta.storageKey()));
        storageService.delete("tenant-1", meta.storageKey());
        assertFalse(storageService.exists("tenant-1", meta.storageKey()));
    }

    @Test
    void generatePresignedUrlRequiresFileToExist() {
        byte[] content = "Document".getBytes(StandardCharsets.UTF_8);
        StoredFileMetadata meta = storageService.store("tenant-1", "doc.pdf", "application/pdf", content);

        String url = storageService.generatePresignedUrl("tenant-1", meta.storageKey(), Duration.ofMinutes(15));
        assertTrue(url.contains(meta.storageKey()));
        assertTrue(url.contains("tenant-1"));

        assertThrows(StorageException.class, () ->
                storageService.generatePresignedUrl("tenant-1", "non-existent-key", Duration.ofMinutes(15)));
    }
}
