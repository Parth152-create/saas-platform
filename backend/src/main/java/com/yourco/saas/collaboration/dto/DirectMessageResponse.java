package com.yourco.saas.collaboration.dto;

import com.yourco.saas.domain.collaboration.DirectMessage;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String senderEmail,
        String content,
        Instant readAt,
        Instant createdAt
) {
    public static DirectMessageResponse fromEntity(DirectMessage msg, String senderName, String senderEmail) {
        return new DirectMessageResponse(
                msg.getId(),
                msg.getConversationId(),
                msg.getSenderId(),
                senderName != null ? senderName : "User",
                senderEmail != null ? senderEmail : "",
                msg.getContent(),
                msg.getReadAt(),
                msg.getCreatedAt()
        );
    }
}
