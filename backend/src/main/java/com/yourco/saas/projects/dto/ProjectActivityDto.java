package com.yourco.saas.projects.dto;

import java.time.Instant;

public record ProjectActivityDto(
        String id,
        String user,
        String action,
        String target,
        String timestamp,
        String type,
        Instant createdAt
) {}
