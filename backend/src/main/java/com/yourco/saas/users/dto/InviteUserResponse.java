package com.yourco.saas.users.dto;

import com.yourco.saas.domain.user.Role;

import java.time.Instant;

public record InviteUserResponse(String email, Role role, String inviteToken, Instant expiresAt) {}