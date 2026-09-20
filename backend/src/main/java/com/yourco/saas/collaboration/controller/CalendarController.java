package com.yourco.saas.collaboration.controller;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.collaboration.dto.CalendarEventResponse;
import com.yourco.saas.collaboration.dto.CreateCalendarEventRequest;
import com.yourco.saas.collaboration.dto.UpdateCalendarEventRequest;
import com.yourco.saas.collaboration.service.CalendarService;
import com.yourco.saas.domain.billing.Feature;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
@RequiresFeature(Feature.CALENDAR)
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getEvents(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(calendarService.getEvents(from, to, projectId));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventResponse> createEvent(@Valid @RequestBody CreateCalendarEventRequest request) {
        CalendarEventResponse response = calendarService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<CalendarEventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(calendarService.getEventById(id));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<CalendarEventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCalendarEventRequest request) {
        return ResponseEntity.ok(calendarService.updateEvent(id, request));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        calendarService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
