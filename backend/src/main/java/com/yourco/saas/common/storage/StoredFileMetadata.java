package com.yourco.saas.common.storage;

import java.time.Instant;

public record StoredFileMetadata(
        String storageKey,
        String originalFilename,
        String sanitizedFilename,
        String contentType,
        long sizeBytes,
        String tenantId,
        Instant uploadedAt
) {}
