package com.yourco.saas.projects.dto;

import com.yourco.saas.domain.projects.ProjectMember;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberDto(
        UUID id,
        UUID memberId,
        String memberName,
        String memberEmail,
        String role,
        Instant joinedAt
) {
    public static ProjectMemberDto fromEntity(ProjectMember member) {
        return new ProjectMemberDto(
                member.getId(),
                member.getMemberId(),
                member.getMemberName(),
                member.getMemberEmail(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
