package com.yourco.saas.collaboration.dto;

import java.util.UUID;

public record CalendarEventAttendeeDto(
        UUID userId,
        String name,
        String email,
        String status
) {}
