package com.yourco.saas.domain.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    @Query("SELECT e FROM CalendarEvent e WHERE e.startAt <= :to AND e.endAt >= :from ORDER BY e.startAt ASC")
    List<CalendarEvent> findInDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e FROM CalendarEvent e WHERE e.projectId = :projectId AND e.startAt <= :to AND e.endAt >= :from ORDER BY e.startAt ASC")
    List<CalendarEvent> findByProjectIdInDateRange(@Param("projectId") UUID projectId,
                                                  @Param("from") Instant from,
                                                  @Param("to") Instant to);

    List<CalendarEvent> findByProjectId(UUID projectId);
}
