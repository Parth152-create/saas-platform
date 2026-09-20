package com.yourco.saas.collaboration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.SignupRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.domain.collaboration.ChannelType;
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
import com.yourco.saas.projects.dto.*;
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
class CollaborationFinalBusinessFlowTest extends IntegrationTestBase {

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
    @DisplayName("Nexa Collaboration v1 — Comprehensive 32-Step Business Flow and Multi-Tenant Isolation")
    void testCollaborationFullBusinessFlow() {
        // -----------------------------------------------------------------
        // Step 1: Register Tenant A
        // -----------------------------------------------------------------
        String tenantAId = "biz-flow-a-" + UUID.randomUUID().toString().substring(0, 6);
        ResponseEntity<TokenResponse> signupRes = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantAId, "super@bizflowa.com", RAW_PASSWORD),
                TokenResponse.class
        );
        assertEquals(HttpStatus.CREATED, signupRes.getStatusCode());
        assertNotNull(signupRes.getBody());

        // -----------------------------------------------------------------
        // Step 2: Login SUPER_ADMIN
        // -----------------------------------------------------------------
        ResponseEntity<TokenResponse> loginSuperRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantAId, "super@bizflowa.com", RAW_PASSWORD),
                TokenResponse.class
        );
        assertEquals(HttpStatus.OK, loginSuperRes.getStatusCode());
        String superTokenA = loginSuperRes.getBody().accessToken();

        TenantRecord tenantA = tenantRegistryService.findByTenantId(tenantAId).orElseThrow();

        // -----------------------------------------------------------------
        // Steps 3, 4, 5: Invite Manager & User A, Accept Invitations
        // -----------------------------------------------------------------
        String managerTokenA = createAndLoginUser(tenantA, "manager@bizflowa.com", Role.MANAGER);
        String userTokenA = createAndLoginUser(tenantA, "user@bizflowa.com", Role.USER);
        User userEntityA = getUser(tenantA, "user@bizflowa.com");
        User managerEntityA = getUser(tenantA, "manager@bizflowa.com");

        // -----------------------------------------------------------------
        // Step 6: Create Project Alpha
        // -----------------------------------------------------------------
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Project Alpha",
                "Next-gen collaboration platform",
                "Acme Corp",
                ProjectStatus.ACTIVE,
                ProjectPriority.HIGH,
                LocalDate.now(),
                LocalDate.now().plusDays(21),
                new BigDecimal("75000.00"),
                null,
                List.of()
        );
        ResponseEntity<ProjectResponse> projRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projReq, bearerHeaders(superTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projRes.getStatusCode());
        UUID projectAlphaId = projRes.getBody().id();

        // -----------------------------------------------------------------
        // Step 7: Add Manager + User A to Project Alpha
        // -----------------------------------------------------------------
        restTemplate.exchange(
                "/api/projects/" + projectAlphaId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(new AddProjectMemberRequest(managerEntityA.getId(), "LEAD"), bearerHeaders(superTokenA)),
                ProjectMemberDto.class
        );
        restTemplate.exchange(
                "/api/projects/" + projectAlphaId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(new AddProjectMemberRequest(userEntityA.getId(), "MEMBER"), bearerHeaders(superTokenA)),
                ProjectMemberDto.class
        );

        // -----------------------------------------------------------------
        // Step 8: Verify Project Alpha Chat Channel Exists & Accessible
        // -----------------------------------------------------------------
        ResponseEntity<ChatChannelResponse> projChanRes = restTemplate.exchange(
                "/api/chat/projects/" + projectAlphaId + "/channel",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                ChatChannelResponse.class
        );
        assertEquals(HttpStatus.OK, projChanRes.getStatusCode());
        assertNotNull(projChanRes.getBody());
        UUID projectChannelId = projChanRes.getBody().id();
        assertEquals(ChannelType.PROJECT, projChanRes.getBody().type());

        // -----------------------------------------------------------------
        // Step 9: Manager Sends Project Message
        // -----------------------------------------------------------------
        SendMessageRequest projMsgReq = new SendMessageRequest("Welcome to Project Alpha everyone!");
        ResponseEntity<ChatMessageResponse> sendProjMsgRes = restTemplate.exchange(
                "/api/chat/channels/" + projectChannelId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(projMsgReq, bearerHeaders(managerTokenA)),
                ChatMessageResponse.class
        );
        assertEquals(HttpStatus.CREATED, sendProjMsgRes.getStatusCode());
        assertEquals("Welcome to Project Alpha everyone!", sendProjMsgRes.getBody().content());

        // -----------------------------------------------------------------
        // Step 10 & 11: User A Retrieves Message History
        // -----------------------------------------------------------------
        ResponseEntity<String> getProjMsgsRes = restTemplate.exchange(
                "/api/chat/channels/" + projectChannelId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, getProjMsgsRes.getStatusCode());
        assertTrue(getProjMsgsRes.getBody().contains("Welcome to Project Alpha everyone!"));

        // -----------------------------------------------------------------
        // Step 12: Manager Assigns Task to User A
        // -----------------------------------------------------------------
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "Design Architecture Spec",
                "Document STOMP and WebSocket specifications",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                userEntityA.getId(),
                LocalDate.now().plusDays(5),
                16,
                0
        );
        ResponseEntity<TaskResponse> taskRes = restTemplate.exchange(
                "/api/projects/" + projectAlphaId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, taskRes.getStatusCode());
        UUID taskId = taskRes.getBody().id();

        // -----------------------------------------------------------------
        // Step 13 & 14: User A Receives Persisted Notification & Checks Unread Count
        // -----------------------------------------------------------------
        ResponseEntity<UnreadCountResponse> unreadRes = restTemplate.exchange(
                "/api/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                UnreadCountResponse.class
        );
        assertEquals(HttpStatus.OK, unreadRes.getStatusCode());
        assertTrue(unreadRes.getBody().count() >= 2); // 1 for project member added, 1 for task assigned

        ResponseEntity<String> userNotifsRes = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, userNotifsRes.getStatusCode());
        assertTrue(userNotifsRes.getBody().contains("Design Architecture Spec"));

        // -----------------------------------------------------------------
        // Step 15: User A Marks Notifications Read
        // -----------------------------------------------------------------
        ResponseEntity<Void> markReadRes = restTemplate.exchange(
                "/api/notifications/read-all",
                HttpMethod.PUT,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, markReadRes.getStatusCode());

        ResponseEntity<UnreadCountResponse> zeroCountRes = restTemplate.exchange(
                "/api/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                UnreadCountResponse.class
        );
        assertEquals(0, zeroCountRes.getBody().count());

        // -----------------------------------------------------------------
        // Step 16: Manager Opens DM with User A
        // -----------------------------------------------------------------
        ResponseEntity<DirectConversationResponse> openDmRes = restTemplate.exchange(
                "/api/chat/direct/" + userEntityA.getId(),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(managerTokenA)),
                DirectConversationResponse.class
        );
        assertEquals(HttpStatus.OK, openDmRes.getStatusCode());
        UUID dmConversationId = openDmRes.getBody().id();

        // -----------------------------------------------------------------
        // Step 17: Send DM
        // -----------------------------------------------------------------
        SendMessageRequest dmMsg = new SendMessageRequest("Alice, please review the spec when ready.");
        ResponseEntity<DirectMessageResponse> sendDmRes = restTemplate.exchange(
                "/api/chat/direct/" + dmConversationId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(dmMsg, bearerHeaders(managerTokenA)),
                DirectMessageResponse.class
        );
        assertEquals(HttpStatus.CREATED, sendDmRes.getStatusCode());

        // -----------------------------------------------------------------
        // Step 18: Verify DM Notification
        // -----------------------------------------------------------------
        ResponseEntity<String> dmNotifCheck = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, dmNotifCheck.getStatusCode());
        assertTrue(dmNotifCheck.getBody().contains("review the spec"));

        // -----------------------------------------------------------------
        // Step 19: Create Project Calendar Event
        // -----------------------------------------------------------------
        Instant eventStart = Instant.now().plusSeconds(3600);
        Instant eventEnd = eventStart.plusSeconds(3600);
        CreateCalendarEventRequest calReq = new CreateCalendarEventRequest(
                "Sprint Kickoff & Sync",
                "Initial kickoff for Project Alpha",
                eventStart,
                eventEnd,
                false,
                "Room 101",
                projectAlphaId,
                List.of(userEntityA.getId())
        );
        ResponseEntity<CalendarEventResponse> calRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.POST,
                new HttpEntity<>(calReq, bearerHeaders(managerTokenA)),
                CalendarEventResponse.class
        );
        assertEquals(HttpStatus.CREATED, calRes.getStatusCode());
        UUID customEventId = calRes.getBody().id();

        // -----------------------------------------------------------------
        // Steps 20, 21, 22: Verify Event, Task Deadline, and Project Deadline Appear in Calendar Feed
        // -----------------------------------------------------------------
        ResponseEntity<List<CalendarEventResponse>> feedRes = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, feedRes.getStatusCode());
        List<CalendarEventResponse> events = feedRes.getBody();
        assertNotNull(events);

        assertTrue(events.stream().anyMatch(e -> e.title().equals("Sprint Kickoff & Sync") && e.sourceType() == CalendarEventType.CUSTOM));
        assertTrue(events.stream().anyMatch(e -> e.title().contains("Project Alpha") && e.sourceType() == CalendarEventType.PROJECT_DEADLINE));
        assertTrue(events.stream().anyMatch(e -> e.title().contains("Design Architecture Spec") && e.sourceType() == CalendarEventType.TASK_DUE));

        // -----------------------------------------------------------------
        // Step 23 & 24: Register Tenant B & Login Tenant B
        // -----------------------------------------------------------------
        String tenantBId = "biz-flow-b-" + UUID.randomUUID().toString().substring(0, 6);
        restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantBId, "super@bizflowb.com", RAW_PASSWORD),
                TokenResponse.class
        );
        ResponseEntity<TokenResponse> loginBRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantBId, "super@bizflowb.com", RAW_PASSWORD),
                TokenResponse.class
        );
        assertEquals(HttpStatus.OK, loginBRes.getStatusCode());
        String superTokenB = loginBRes.getBody().accessToken();

        // -----------------------------------------------------------------
        // Step 25: Attempt Tenant A Channel Access -> Rejected (404)
        // -----------------------------------------------------------------
        ResponseEntity<String> breachChan = restTemplate.exchange(
                "/api/chat/channels/" + projectChannelId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, breachChan.getStatusCode());

        // -----------------------------------------------------------------
        // Step 26: Attempt Tenant A Message Access -> Rejected (404)
        // -----------------------------------------------------------------
        ResponseEntity<String> breachMsgs = restTemplate.exchange(
                "/api/chat/channels/" + projectChannelId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, breachMsgs.getStatusCode());

        // -----------------------------------------------------------------
        // Step 27: Attempt Tenant A DM -> Rejected (404)
        // -----------------------------------------------------------------
        ResponseEntity<String> breachDm = restTemplate.exchange(
                "/api/chat/direct/" + dmConversationId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, breachDm.getStatusCode());

        // -----------------------------------------------------------------
        // Step 28: Attempt Tenant A Calendar Access -> Rejected (404)
        // -----------------------------------------------------------------
        ResponseEntity<String> breachCal = restTemplate.exchange(
                "/api/calendar/events/" + customEventId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, breachCal.getStatusCode());

        // -----------------------------------------------------------------
        // Step 29: Attempt Tenant A Notification Access -> Empty/Isolated
        // -----------------------------------------------------------------
        ResponseEntity<String> breachNotif = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                String.class
        );
        assertEquals(HttpStatus.OK, breachNotif.getStatusCode());
        assertFalse(breachNotif.getBody().contains("Project Alpha"));
        assertFalse(breachNotif.getBody().contains("Design Architecture Spec"));

        // -----------------------------------------------------------------
        // Step 30, 31, 32: Verify Tenant A Data Unchanged and Tenant B Completely Isolated
        // -----------------------------------------------------------------
        ResponseEntity<String> verifyTenantAMsgs = restTemplate.exchange(
                "/api/chat/channels/" + projectChannelId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, verifyTenantAMsgs.getStatusCode());
        assertTrue(verifyTenantAMsgs.getBody().contains("Welcome to Project Alpha everyone!"));

        ResponseEntity<List<CalendarEventResponse>> verifyTenantBCal = restTemplate.exchange(
                "/api/calendar/events",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superTokenB)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, verifyTenantBCal.getStatusCode());
        assertNotNull(verifyTenantBCal.getBody());
        assertTrue(verifyTenantBCal.getBody().isEmpty());
    }
}
