package com.yourco.saas.users.dto;

import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        UserStatus status,
        String inviteToken,
        Instant inviteTokenExpiresAt,
        Instant createdAt
) {}
