package com.yourco.saas.collaboration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.domain.collaboration.ChannelType;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.projects.dto.CreateProjectRequest;
import com.yourco.saas.projects.dto.ProjectResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class ChatIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

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
    @DisplayName("Chat Channels, Direct Messages, RBAC, Message Validation, and Cross-Tenant Isolation")
    void testChatAndDirectMessagingLifecycle() {
        TenantRecord tenantA = provisionTenant("chat-a");
        TenantRecord tenantB = provisionTenant("chat-b");

        String adminTokenA = createAndLoginUser(tenantA, "admin@tenanta.com", Role.ADMIN);
        String managerTokenA = createAndLoginUser(tenantA, "manager@tenanta.com", Role.MANAGER);
        String userTokenA = createAndLoginUser(tenantA, "user@tenanta.com", Role.USER);
        User userEntityA = getUser(tenantA, "user@tenanta.com");
        User managerEntityA = getUser(tenantA, "manager@tenanta.com");

        String adminTokenB = createAndLoginUser(tenantB, "admin@tenantb.com", Role.ADMIN);

        // 1. Channels: Default channels auto-seeded (general, random)
        ResponseEntity<List<ChatChannelResponse>> channelsRes = restTemplate.exchange(
                "/api/chat/channels",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, channelsRes.getStatusCode());
        assertNotNull(channelsRes.getBody());
        assertTrue(channelsRes.getBody().size() >= 2);
        UUID generalChannelId = channelsRes.getBody().stream()
                .filter(c -> "general".equalsIgnoreCase(c.name()))
                .findFirst().orElseThrow().id();

        // 2. Channel Creation: Regular user cannot create channel (403)
        CreateChannelRequest channelReq = new CreateChannelRequest("engineering", "Tech discussion", ChannelType.WORKSPACE, null);
        ResponseEntity<String> userCreateChan = restTemplate.exchange(
                "/api/chat/channels",
                HttpMethod.POST,
                new HttpEntity<>(channelReq, bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userCreateChan.getStatusCode());

        // 3. Channel Creation: Manager creates channel
        ResponseEntity<ChatChannelResponse> managerCreateChan = restTemplate.exchange(
                "/api/chat/channels",
                HttpMethod.POST,
                new HttpEntity<>(channelReq, bearerHeaders(managerTokenA)),
                ChatChannelResponse.class
        );
        assertEquals(HttpStatus.CREATED, managerCreateChan.getStatusCode());
        assertNotNull(managerCreateChan.getBody());
        UUID engineeringChannelId = managerCreateChan.getBody().id();

        // 4. Project Channel Creation/Auto-provisioning
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Nexa Alpha", "First project", "Acme",
                ProjectStatus.ACTIVE, ProjectPriority.HIGH,
                LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("10000.00"), null, List.of()
        );
        ResponseEntity<ProjectResponse> projRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projReq, bearerHeaders(managerTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projRes.getStatusCode());
        UUID projectId = projRes.getBody().id();

        ResponseEntity<ChatChannelResponse> projChannelRes = restTemplate.exchange(
                "/api/chat/projects/" + projectId + "/channel",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                ChatChannelResponse.class
        );
        assertEquals(HttpStatus.OK, projChannelRes.getStatusCode());
        assertNotNull(projChannelRes.getBody());
        assertEquals(ChannelType.PROJECT, projChannelRes.getBody().type());
        assertEquals(projectId, projChannelRes.getBody().projectId());

        // 5. Send Channel Message: Blank message rejected (400)
        ResponseEntity<String> blankMsgRes = restTemplate.exchange(
                "/api/chat/channels/" + engineeringChannelId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(new SendMessageRequest("   "), bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, blankMsgRes.getStatusCode());

        // 6. Send Channel Message: Valid message posted
        SendMessageRequest validMsgReq = new SendMessageRequest("Hello team, check out the new release!");
        ResponseEntity<ChatMessageResponse> sendMsgRes = restTemplate.exchange(
                "/api/chat/channels/" + engineeringChannelId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(validMsgReq, bearerHeaders(userTokenA)),
                ChatMessageResponse.class
        );
        assertEquals(HttpStatus.CREATED, sendMsgRes.getStatusCode());
        assertNotNull(sendMsgRes.getBody());
        assertEquals("Hello team, check out the new release!", sendMsgRes.getBody().content());

        // 7. Get Channel Messages: Verify message history
        ResponseEntity<String> getMsgsRes = restTemplate.exchange(
                "/api/chat/channels/" + engineeringChannelId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, getMsgsRes.getStatusCode());
        assertTrue(getMsgsRes.getBody().contains("Hello team, check out the new release!"));

        // 8. Direct Messages: Manager opens DM with User A
        ResponseEntity<DirectConversationResponse> openDmRes = restTemplate.exchange(
                "/api/chat/direct/" + userEntityA.getId(),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(managerTokenA)),
                DirectConversationResponse.class
        );
        assertEquals(HttpStatus.OK, openDmRes.getStatusCode());
        assertNotNull(openDmRes.getBody());
        UUID conversationId = openDmRes.getBody().id();

        // 9. Conversation Uniqueness: Opening DM again returns identical conversation ID
        ResponseEntity<DirectConversationResponse> openDmAgainRes = restTemplate.exchange(
                "/api/chat/direct/" + managerEntityA.getId(),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                DirectConversationResponse.class
        );
        assertEquals(HttpStatus.OK, openDmAgainRes.getStatusCode());
        assertEquals(conversationId, openDmAgainRes.getBody().id());

        // 10. Send Direct Message
        SendMessageRequest dmReq = new SendMessageRequest("Hey Alice, do you have a minute?");
        ResponseEntity<DirectMessageResponse> sendDmRes = restTemplate.exchange(
                "/api/chat/direct/" + conversationId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(dmReq, bearerHeaders(managerTokenA)),
                DirectMessageResponse.class
        );
        assertEquals(HttpStatus.CREATED, sendDmRes.getStatusCode());
        assertEquals("Hey Alice, do you have a minute?", sendDmRes.getBody().content());

        // 11. Retrieve DM messages: User A fetches history
        ResponseEntity<String> getDmRes = restTemplate.exchange(
                "/api/chat/direct/" + conversationId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, getDmRes.getStatusCode());
        assertTrue(getDmRes.getBody().contains("Hey Alice, do you have a minute?"));

        // 12. Cross-Tenant Isolation: Tenant B cannot access Tenant A channel
        ResponseEntity<String> crossChanRes = restTemplate.exchange(
                "/api/chat/channels/" + engineeringChannelId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossChanRes.getStatusCode());

        // 13. Cross-Tenant Isolation: Tenant B cannot send to Tenant A channel
        ResponseEntity<String> crossSendChan = restTemplate.exchange(
                "/api/chat/channels/" + engineeringChannelId + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(new SendMessageRequest("Infiltration"), bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossSendChan.getStatusCode());

        // 14. Cross-Tenant Isolation: Tenant B cannot access Tenant A DM conversation
        ResponseEntity<String> crossDmRes = restTemplate.exchange(
                "/api/chat/direct/" + conversationId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossDmRes.getStatusCode());

        // 15. Cross-Tenant Isolation: Tenant B cannot open DM with Tenant A user
        ResponseEntity<String> crossOpenDm = restTemplate.exchange(
                "/api/chat/direct/" + userEntityA.getId(),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossOpenDm.getStatusCode());
    }
}
