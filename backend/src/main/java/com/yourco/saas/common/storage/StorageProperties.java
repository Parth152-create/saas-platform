package com.yourco.saas.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "memory"; // memory, local, s3
    private String basePath = "/tmp/nexa-storage";
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB default

    private Set<String> allowedMimeTypes = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "text/csv",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }
    public Set<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) { this.allowedMimeTypes = allowedMimeTypes; }
}
