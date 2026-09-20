package com.yourco.saas.domain.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarEventAttendeeRepository extends JpaRepository<CalendarEventAttendee, UUID> {

    List<CalendarEventAttendee> findByEventId(UUID eventId);

    List<CalendarEventAttendee> findByUserId(UUID userId);

    void deleteByEventId(UUID eventId);
}
