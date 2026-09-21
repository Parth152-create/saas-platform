package com.yourco.saas.bulk.dto;

import java.util.List;
import java.util.UUID;

public record BulkOperationResultDto(
        int totalRequested,
        int successCount,
        int failureCount,
        List<UUID> successIds,
        List<BulkItemErrorDto> errors
) {}
