package com.yourco.saas.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateCalendarEventRequest(
        @NotBlank(message = "Event title is required")
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @NotNull(message = "Start time is required")
        Instant startAt,

        @NotNull(message = "End time is required")
        Instant endAt,

        boolean allDay,

        @Size(max = 255, message = "Location cannot exceed 255 characters")
        String location,

        UUID projectId,

        List<UUID> attendeeIds
) {}
