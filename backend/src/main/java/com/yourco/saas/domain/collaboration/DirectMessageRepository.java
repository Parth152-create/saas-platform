package com.yourco.saas.domain.collaboration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    Optional<DirectMessage> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID currentUserId);

    @Modifying
    @Query("UPDATE DirectMessage m SET m.readAt = :readAt WHERE m.conversationId = :conversationId AND m.senderId != :currentUserId AND m.readAt IS NULL")
    int markAllReadForConversation(@Param("conversationId") UUID conversationId,
                                   @Param("currentUserId") UUID currentUserId,
                                   @Param("readAt") Instant readAt);
}
