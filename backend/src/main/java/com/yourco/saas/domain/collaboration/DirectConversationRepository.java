package com.yourco.saas.domain.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    @Query("SELECT d FROM DirectConversation d WHERE (d.userOneId = :u1 AND d.userTwoId = :u2) OR (d.userOneId = :u2 AND d.userTwoId = :u1)")
    Optional<DirectConversation> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);

    @Query("SELECT d FROM DirectConversation d WHERE d.userOneId = :userId OR d.userTwoId = :userId ORDER BY d.updatedAt DESC")
    List<DirectConversation> findForUser(@Param("userId") UUID userId);
}
