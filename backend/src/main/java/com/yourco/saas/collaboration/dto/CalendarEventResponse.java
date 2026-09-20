package com.yourco.saas.collaboration.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        String location,
        UUID projectId,
        String projectName,
        UUID createdBy,
        CalendarEventType sourceType,
        List<CalendarEventAttendeeDto> attendees,
        Instant createdAt
) {}
