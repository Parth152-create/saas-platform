package com.yourco.saas.tenant;

public record TenantRecord(Long id, String tenantId, String schemaName, String status, String plan) {}