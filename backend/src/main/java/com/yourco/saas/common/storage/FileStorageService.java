package com.yourco.saas.common.storage;

import java.time.Duration;

public interface FileStorageService {
    StoredFileMetadata store(String tenantId, String originalFilename, String contentType, byte[] content);
    byte[] retrieve(String tenantId, String storageKey);
    void delete(String tenantId, String storageKey);
    boolean exists(String tenantId, String storageKey);
    String generatePresignedUrl(String tenantId, String storageKey, Duration expiration);
}
