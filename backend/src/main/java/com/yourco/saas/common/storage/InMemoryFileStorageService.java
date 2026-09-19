package com.yourco.saas.common.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableConfigurationProperties(StorageProperties.class)
public class InMemoryFileStorageService implements FileStorageService {

    private final StorageProperties properties;

    // Tenant-isolated file registry: tenantId -> (storageKey -> StoredItem)
    private final Map<String, Map<String, StoredItem>> tenantStores = new ConcurrentHashMap<>();

    public InMemoryFileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredFileMetadata store(String tenantId, String originalFilename, String contentType, byte[] content) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required for file storage");
        }
        if (content == null) {
            throw new InvalidFileException("File content cannot be null");
        }

        FileValidationUtils.validateFile(originalFilename, contentType, content.length, properties);
        String sanitized = FileValidationUtils.sanitizeFilename(originalFilename);
        String storageKey = UUID.randomUUID() + "-" + sanitized;

        StoredFileMetadata metadata = new StoredFileMetadata(
                storageKey,
                originalFilename,
                sanitized,
                contentType,
                content.length,
                tenantId,
                Instant.now()
        );

        tenantStores.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                .put(storageKey, new StoredItem(metadata, content.clone()));

        return metadata;
    }

    @Override
    public byte[] retrieve(String tenantId, String storageKey) {
        if (tenantId == null || storageKey == null) {
            throw new StorageException("Tenant ID and storage key are required");
        }

        Map<String, StoredItem> tenantStore = tenantStores.get(tenantId);
        if (tenantStore == null) {
            throw new StorageException("File not found or access denied: " + storageKey);
        }

        StoredItem item = tenantStore.get(storageKey);
        if (item == null) {
            throw new StorageException("File not found or access denied: " + storageKey);
        }

        return item.content().clone();
    }

    @Override
    public void delete(String tenantId, String storageKey) {
        if (tenantId == null || storageKey == null) {
            return;
        }
        Map<String, StoredItem> tenantStore = tenantStores.get(tenantId);
        if (tenantStore != null) {
            tenantStore.remove(storageKey);
        }
    }

    @Override
    public boolean exists(String tenantId, String storageKey) {
        if (tenantId == null || storageKey == null) {
            return false;
        }
        Map<String, StoredItem> tenantStore = tenantStores.get(tenantId);
        return tenantStore != null && tenantStore.containsKey(storageKey);
    }

    @Override
    public String generatePresignedUrl(String tenantId, String storageKey, Duration expiration) {
        if (!exists(tenantId, storageKey)) {
            throw new StorageException("Cannot generate presigned URL for non-existent file: " + storageKey);
        }
        return "/api/storage/" + tenantId + "/download/" + storageKey + "?expires=" + expiration.toSeconds();
    }

    private record StoredItem(StoredFileMetadata metadata, byte[] content) {}
}
