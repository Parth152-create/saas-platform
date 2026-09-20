package com.yourco.saas.collaboration.dto;

import com.yourco.saas.domain.collaboration.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateChannelRequest(
        @NotBlank(message = "Channel name is required")
        @Size(min = 1, max = 100, message = "Channel name must be between 1 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        ChannelType type,

        UUID projectId
) {}
