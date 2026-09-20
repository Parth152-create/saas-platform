package com.yourco.saas.collaboration.dto;

import com.yourco.saas.domain.collaboration.Notification;
import com.yourco.saas.domain.collaboration.NotificationType;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String resourceType,
        String resourceId,
        String targetPath,
        boolean read,
        Instant createdAt,
        String timestamp
) {
    public static NotificationResponse fromEntity(Notification n) {
        String relativeTime = formatRelativeTime(n.getCreatedAt());
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getResourceType(),
                n.getResourceId(),
                n.getTargetPath(),
                n.isRead(),
                n.getCreatedAt(),
                relativeTime
        );
    }

    private static String formatRelativeTime(Instant instant) {
        if (instant == null) return "recently";
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
