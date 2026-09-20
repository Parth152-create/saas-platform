package com.yourco.saas.collaboration.dto;

import java.time.Instant;
import java.util.UUID;

public record DirectConversationResponse(
        UUID id,
        UUID otherUserId,
        String otherUserName,
        String otherUserEmail,
        String otherUserRole,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        Instant createdAt
) {}
