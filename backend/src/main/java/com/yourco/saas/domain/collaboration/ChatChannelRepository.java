package com.yourco.saas.domain.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

    Optional<ChatChannel> findByProjectId(UUID projectId);

    List<ChatChannel> findByTypeOrderByCreatedAtAsc(ChannelType type);

    @Query("SELECT c FROM ChatChannel c WHERE c.type = 'WORKSPACE' OR c.projectId IN :projectIds ORDER BY c.createdAt ASC")
    List<ChatChannel> findWorkspaceAndUserProjects(@Param("projectIds") List<UUID> projectIds);
}
