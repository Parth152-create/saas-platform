package com.yourco.saas.collaboration.dto;

import com.yourco.saas.domain.collaboration.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID channelId,
        UUID senderId,
        String senderName,
        String senderEmail,
        String content,
        Instant createdAt
) {
    public static ChatMessageResponse fromEntity(ChatMessage message, String senderName, String senderEmail) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChannelId(),
                message.getSenderId(),
                senderName != null ? senderName : "User",
                senderEmail != null ? senderEmail : "",
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
