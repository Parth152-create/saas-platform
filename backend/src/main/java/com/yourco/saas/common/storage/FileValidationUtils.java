package com.yourco.saas.common.storage;

import java.nio.file.Paths;

public final class FileValidationUtils {

    private FileValidationUtils() {}

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }

        // Strip path navigation and directory components (support both / and \)
        String normalized = filename.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String cleanName = (lastSlash >= 0) ? normalized.substring(lastSlash + 1) : normalized;

        // Remove null bytes
        cleanName = cleanName.replace("\0", "");
        // Remove directory traversal sequences
        cleanName = cleanName.replace("..", "");
        // Replace spaces and special characters except dot, dash, underscore
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        if (cleanName.startsWith(".")) {
            cleanName = "file" + cleanName;
        }

        if (cleanName.length() > 100) {
            int extDot = cleanName.lastIndexOf('.');
            String ext = (extDot > 0 && extDot < cleanName.length()) ? cleanName.substring(extDot) : "";
            cleanName = cleanName.substring(0, Math.min(cleanName.length(), 90)) + ext;
        }

        return cleanName.isBlank() ? "unnamed_file" : cleanName;
    }

    public static void validateFile(String filename, String contentType, long sizeBytes, StorageProperties properties) {
        if (sizeBytes <= 0) {
            throw new InvalidFileException("File cannot be empty");
        }

        if (sizeBytes > properties.getMaxFileSizeBytes()) {
            throw new FileSizeExceededException(String.format(
                    "File size %d bytes exceeds maximum allowed limit of %d bytes",
                    sizeBytes, properties.getMaxFileSizeBytes()));
        }

        if (contentType == null || contentType.isBlank()) {
            throw new InvalidFileException("Content type header is missing or empty");
        }

        String normalizedType = contentType.split(";")[0].trim().toLowerCase();
        if (!properties.getAllowedMimeTypes().contains(normalizedType)) {
            throw new InvalidFileException("Unsupported MIME type: " + normalizedType);
        }
    }

    public static String buildTenantStoragePath(String tenantId, String storageKey) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required for storage path isolation");
        }
        return "tenants/" + tenantId + "/" + storageKey;
    }
}
