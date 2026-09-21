package com.yourco.saas.search.dto;

public record SearchResultDto(
        String id,
        String type,
        String title,
        String subtitle,
        String description,
        String targetUrl,
        String status
) {}
