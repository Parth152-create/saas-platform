package com.yourco.saas.users.dto;

import com.yourco.saas.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotNull Role role
) {}