package com.yourco.saas.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content cannot be blank")
        @Size(min = 1, max = 4000, message = "Message content must be between 1 and 4000 characters")
        String content
) {}
