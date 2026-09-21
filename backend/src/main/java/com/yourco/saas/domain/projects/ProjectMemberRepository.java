package com.yourco.saas.domain.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByMemberId(UUID memberId);

    Optional<ProjectMember> findByProjectIdAndMemberId(UUID projectId, UUID memberId);

    void deleteByProjectIdAndMemberId(UUID projectId, UUID memberId);

    boolean existsByProjectIdAndMemberId(UUID projectId, UUID memberId);

    long countByProjectId(UUID projectId);
}
