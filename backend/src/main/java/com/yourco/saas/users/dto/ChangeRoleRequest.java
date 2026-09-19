package com.yourco.saas.users.dto;

import com.yourco.saas.domain.user.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}
