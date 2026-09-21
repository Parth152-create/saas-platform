package com.yourco.saas.selfservice.dto;

public record UpdateSelfServiceProfileRequest(
        String name,
        String phone,
        String location,
        String workModel,
        // Protected fields - if provided by client, must be rejected or strictly ignored!
        String role,
        String status,
        String email,
        String tenantId
) {}
