package com.yourco.saas.collaboration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.projects.dto.CreateProjectRequest;
import com.yourco.saas.projects.dto.CreateTaskRequest;
import com.yourco.saas.projects.dto.ProjectResponse;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class CalendarIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createAndLoginUser(TenantRecord tenant, String email, Role role) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> res = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        return res.getBody().accessToken();
    }

    private User getUser(TenantRecord tenant, String email) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            return userRepository.findByEmail(email).orElseThrow();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Calendar Events CRUD, Range Queries, Validations, Projections, and Tenant Isolation")
    void testCalendarFeedAndProjections() {
        TenantRecord tenantA = provisionTenant("cal-a");
        TenantRecord tenantB = provisionTenant("cal-b");

        String managerTokenA = createAndLoginUser(tenantA, "manager@tenanta.com", Role.MANAGER);
        String userTokenA = createAndLoginUser(tenantA, "user@tenanta.com", Role.USER);
        User userEntityA = getUser(tenantA, "user@tenanta.com");

        String adminTokenB = createAndLoginUser(tenantB, "admin@tenantb.com", Role.ADMIN);

        // 1. Time Validation: endAt before startAt -> 400 Bad Request
        Instant now = Instant.now();
        CreateCalendarEventRequest invalidTimeReq = new CreateCalendarEventRequest(
                "Time Warp", "Desc",
                now.plusSeconds(3600), now, // start after end
                false, "Room 1", null, List.of()
        );
        ResponseEntity<String> invalidRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.POST,
                new HttpEntity<>(invalidTimeReq, bearerHeaders(managerTokenA)),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidRes.getStatusCode());

        // 2. Attendee Validation: Non-existent or cross-tenant user ID -> 400 Bad Request
        UUID fakeUserId = UUID.randomUUID();
        CreateCalendarEventRequest invalidAttReq = new CreateCalendarEventRequest(
                "Sprint Planning", "Desc",
                now, now.plusSeconds(3600),
                false, "Room 1", null, List.of(fakeUserId)
        );
        ResponseEntity<String> invalidAttRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.POST,
                new HttpEntity<>(invalidAttReq, bearerHeaders(managerTokenA)),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidAttRes.getStatusCode());

        // 3. Create Custom Event with Valid Attendee
        CreateCalendarEventRequest validEventReq = new CreateCalendarEventRequest(
                "Sprint 25 Planning", "Architecture review",
                now.plusSeconds(3600), now.plusSeconds(7200),
                false, "Conference Room A", null, List.of(userEntityA.getId())
        );
        ResponseEntity<CalendarEventResponse> createRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.POST,
                new HttpEntity<>(validEventReq, bearerHeaders(managerTokenA)),
                CalendarEventResponse.class
        );
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        assertNotNull(createRes.getBody());
        UUID eventId = createRes.getBody().id();
        assertEquals(CalendarEventType.CUSTOM, createRes.getBody().sourceType());

        // 4. Create Project with Due Date -> Projected as PROJECT_DEADLINE
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Q4 Initiative", "Quarterly goals", "Acme",
                ProjectStatus.ACTIVE, ProjectPriority.HIGH,
                LocalDate.now(), LocalDate.now().plusDays(14),
                new BigDecimal("25000.00"), null, List.of()
        );
        ResponseEntity<ProjectResponse> projRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projReq, bearerHeaders(managerTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projRes.getStatusCode());
        UUID projectId = projRes.getBody().id();

        // 5. Create Task with Due Date -> Projected as TASK_DUE
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "Implement Redis Caching", "Perf improvement",
                TaskStatus.TODO, TaskPriority.HIGH,
                userEntityA.getId(), LocalDate.now().plusDays(7), 8, 0
        );
        ResponseEntity<TaskResponse> taskRes = restTemplate.exchange(
                "/api/projects/" + projectId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, taskRes.getStatusCode());

        // 6. Query Calendar Feed: Verify custom event, project deadline, and task deadline
        ResponseEntity<List<CalendarEventResponse>> feedRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, feedRes.getStatusCode());
        assertNotNull(feedRes.getBody());
        List<CalendarEventResponse> events = feedRes.getBody();

        assertTrue(events.stream().anyMatch(e -> e.title().equals("Sprint 25 Planning") && e.sourceType() == CalendarEventType.CUSTOM));
        assertTrue(events.stream().anyMatch(e -> e.title().contains("Q4 Initiative") && e.sourceType() == CalendarEventType.PROJECT_DEADLINE));
        assertTrue(events.stream().anyMatch(e -> e.title().contains("Implement Redis Caching") && e.sourceType() == CalendarEventType.TASK_DUE));

        // 7. Update Event
        UpdateCalendarEventRequest updateReq = new UpdateCalendarEventRequest(
                "Sprint 25 Planning (Updated)", "Updated details",
                now.plusSeconds(3600), now.plusSeconds(7200),
                false, "Virtual", null, List.of()
        );
        ResponseEntity<CalendarEventResponse> updateRes = restTemplate.exchange(
                "/api/calendar/events/" + eventId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(managerTokenA)),
                CalendarEventResponse.class
        );
        assertEquals(HttpStatus.OK, updateRes.getStatusCode());
        assertEquals("Sprint 25 Planning (Updated)", updateRes.getBody().title());

        // 8. Cross-Tenant Isolation: Tenant B cannot access Tenant A calendar event
        ResponseEntity<String> crossGetRes = restTemplate.exchange(
                "/api/calendar/events/" + eventId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossGetRes.getStatusCode());

        // 9. Cross-Tenant Isolation: Tenant B cannot modify Tenant A calendar event
        ResponseEntity<String> crossPutRes = restTemplate.exchange(
                "/api/calendar/events/" + eventId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossPutRes.getStatusCode());

        // 10. Cross-Tenant Isolation: Tenant B sees clean calendar feed without Tenant A events
        ResponseEntity<List<CalendarEventResponse>> crossFeedRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, crossFeedRes.getStatusCode());
        assertNotNull(crossFeedRes.getBody());
        assertTrue(crossFeedRes.getBody().isEmpty());

        // 11. Delete Event
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                "/api/calendar/events/" + eventId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(managerTokenA)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteRes.getStatusCode());
    }
}
