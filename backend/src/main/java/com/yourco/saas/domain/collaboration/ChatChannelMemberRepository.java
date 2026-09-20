package com.yourco.saas.domain.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatChannelMemberRepository extends JpaRepository<ChatChannelMember, UUID> {

    List<ChatChannelMember> findByChannelId(UUID channelId);

    List<ChatChannelMember> findByUserId(UUID userId);

    Optional<ChatChannelMember> findByChannelIdAndUserId(UUID channelId, UUID userId);

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    void deleteByChannelIdAndUserId(UUID channelId, UUID userId);
}
