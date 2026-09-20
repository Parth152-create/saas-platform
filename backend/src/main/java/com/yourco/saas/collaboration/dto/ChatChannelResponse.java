package com.yourco.saas.collaboration.dto;

import com.yourco.saas.domain.collaboration.ChannelType;
import com.yourco.saas.domain.collaboration.ChatChannel;

import java.time.Instant;
import java.util.UUID;

public record ChatChannelResponse(
        UUID id,
        String name,
        String description,
        ChannelType type,
        UUID projectId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long unreadCount,
        String lastMessage,
        Instant lastMessageAt
) {
    public static ChatChannelResponse fromEntity(ChatChannel channel, long unreadCount, String lastMessage, Instant lastMessageAt) {
        return new ChatChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getType(),
                channel.getProjectId(),
                channel.getCreatedBy(),
                channel.getCreatedAt(),
                channel.getUpdatedAt(),
                unreadCount,
                lastMessage,
                lastMessageAt
        );
    }
}
