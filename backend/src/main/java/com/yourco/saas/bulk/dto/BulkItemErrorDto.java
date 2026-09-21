package com.yourco.saas.bulk.dto;

import java.util.UUID;

public record BulkItemErrorDto(
        UUID id,
        String error
) {}
