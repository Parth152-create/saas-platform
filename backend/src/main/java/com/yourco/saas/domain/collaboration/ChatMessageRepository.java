package com.yourco.saas.domain.collaboration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByChannelIdOrderByCreatedAtDesc(UUID channelId, Pageable pageable);

    Optional<ChatMessage> findFirstByChannelIdOrderByCreatedAtDesc(UUID channelId);

    long countByChannelId(UUID channelId);
}
