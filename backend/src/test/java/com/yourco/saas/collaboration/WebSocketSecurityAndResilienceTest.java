package com.yourco.saas.collaboration;

import com.yourco.saas.auth.JwtService;
import com.yourco.saas.collaboration.websocket.AuthChannelInterceptor;
import com.yourco.saas.domain.collaboration.DirectConversation;
import com.yourco.saas.domain.collaboration.DirectConversationRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WebSocketSecurityAndResilienceTest extends IntegrationTestBase {

    @Autowired
    private AuthChannelInterceptor interceptor;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DirectConversationRepository directConversationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final MessageChannel dummyChannel = mock(MessageChannel.class);

    private User createUser(TenantRecord tenant, String email, Role role) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode("Password123!"));
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        } finally {
            TenantContext.clear();
        }
    }

    private String issueValidToken(User user, String tenantId) {
        return jwtService.issueTokens(user, tenantId).accessToken();
    }

    private Message<?> createConnectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (token != null) {
            accessor.addNativeHeader("Authorization", "Bearer " + token);
        }
        accessor.setSessionAttributes(new ConcurrentHashMap<>());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> createSubscribeMessage(String destination, String tenantId, String tenantSchema, String userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();
        if (tenantId != null) sessionAttributes.put("tenant_id", tenantId);
        if (tenantSchema != null) sessionAttributes.put("tenant_schema", tenantSchema);
        if (userId != null) sessionAttributes.put("user_id", userId);
        accessor.setSessionAttributes(sessionAttributes);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("CONNECT with valid JWT sets authentication and tenant session attributes")
    void validConnectSetsSessionAttributes() {
        TenantRecord tenant = provisionTenant("ws-valid-conn");
        User user = createUser(tenant, "ws-user@test.com", Role.USER);
        String token = issueValidToken(user, tenant.tenantId());

        Message<?> connectMsg = createConnectMessage(token);
        Message<?> result = interceptor.preSend(connectMsg, dummyChannel);

        assertNotNull(result);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals(user.getId().toString(), accessor.getUser().getName());

        Map<String, Object> attrs = accessor.getSessionAttributes();
        assertNotNull(attrs);
        assertEquals(tenant.tenantId(), attrs.get("tenant_id"));
        assertEquals(tenant.schemaName(), attrs.get("tenant_schema"));
        assertEquals(user.getId().toString(), attrs.get("user_id"));
    }

    @Test
    @DisplayName("CONNECT without Authorization header or malformed header is rejected")
    void connectWithoutAuthHeaderRejected() {
        Message<?> noAuth = createConnectMessage(null);
        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(noAuth, dummyChannel));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Basic invalid-format");
        accessor.setSessionAttributes(new ConcurrentHashMap<>());
        Message<?> badAuth = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(badAuth, dummyChannel));
    }

    @Test
    @DisplayName("CONNECT with expired JWT is rejected")
    void connectWithExpiredTokenRejected() {
        TenantRecord tenant = provisionTenant("ws-exp-token");
        User user = createUser(tenant, "expired@test.com", Role.USER);

        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("saas-platform")
                .subject(user.getId().toString())
                .issuedAt(past.minusSeconds(60))
                .expiresAt(past)
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", tenant.tenantId())
                .claim("role", "USER")
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        Message<?> connectMsg = createConnectMessage(expiredToken);
        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(connectMsg, dummyChannel));
    }

    @Test
    @DisplayName("CONNECT with disabled user is rejected")
    void connectWithDisabledUserRejected() {
        TenantRecord tenant = provisionTenant("ws-disabled-user");
        User user = createUser(tenant, "disabled@test.com", Role.USER);
        String token = issueValidToken(user, tenant.tenantId());

        // Mark user disabled in Redis session store
        jwtService.markUserDisabled(user.getId());

        Message<?> connectMsg = createConnectMessage(token);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(connectMsg, dummyChannel));
        assertTrue(ex.getMessage().contains("User is disabled"));

        // Reactivate and verify connection succeeds
        jwtService.markUserEnabled(user.getId());
        Message<?> retryMsg = createConnectMessage(token);
        assertNotNull(interceptor.preSend(retryMsg, dummyChannel));
    }

    @Test
    @DisplayName("SUBSCRIBE to own tenant destination succeeds")
    void subscribeOwnTenantDestinationSucceeds() {
        TenantRecord tenant = provisionTenant("ws-sub-own");
        User user = createUser(tenant, "sub-own@test.com", Role.USER);

        String destination = "/topic/tenant/" + tenant.tenantId() + "/channels/" + UUID.randomUUID();
        Message<?> subMsg = createSubscribeMessage(destination, tenant.tenantId(), tenant.schemaName(), user.getId().toString());

        assertDoesNotThrow(() -> interceptor.preSend(subMsg, dummyChannel));
    }

    @Test
    @DisplayName("SUBSCRIBE to cross-tenant topic is blocked with SecurityException")
    void subscribeCrossTenantDestinationBlocked() {
        TenantRecord tenantA = provisionTenant("ws-iso-a");
        TenantRecord tenantB = provisionTenant("ws-iso-b");
        User userA = createUser(tenantA, "usera@iso.test", Role.USER);

        // User A tries to subscribe to Tenant B's channel
        String crossTenantDestination = "/topic/tenant/" + tenantB.tenantId() + "/channels/" + UUID.randomUUID();
        Message<?> subMsg = createSubscribeMessage(crossTenantDestination, tenantA.tenantId(), tenantA.schemaName(), userA.getId().toString());

        SecurityException ex = assertThrows(SecurityException.class,
                () -> interceptor.preSend(subMsg, dummyChannel));
        assertEquals("Cross-tenant subscription forbidden", ex.getMessage());
    }

    @Test
    @DisplayName("SUBSCRIBE to other user's notifications is blocked with SecurityException")
    void subscribeOtherUserNotificationsBlocked() {
        TenantRecord tenant = provisionTenant("ws-user-notif-iso");
        User user1 = createUser(tenant, "user1@notif.test", Role.USER);
        User user2 = createUser(tenant, "user2@notif.test", Role.USER);

        // User 1 tries to subscribe to User 2's notifications
        String destUser2 = "/topic/tenant/" + tenant.tenantId() + "/users/" + user2.getId() + "/notifications";
        Message<?> badSub = createSubscribeMessage(destUser2, tenant.tenantId(), tenant.schemaName(), user1.getId().toString());

        SecurityException ex = assertThrows(SecurityException.class,
                () -> interceptor.preSend(badSub, dummyChannel));
        assertEquals("Cross-user subscription forbidden", ex.getMessage());

        // User 1 subscribes to own notifications -> succeeds
        String destUser1 = "/topic/tenant/" + tenant.tenantId() + "/users/" + user1.getId() + "/notifications";
        Message<?> goodSub = createSubscribeMessage(destUser1, tenant.tenantId(), tenant.schemaName(), user1.getId().toString());
        assertDoesNotThrow(() -> interceptor.preSend(goodSub, dummyChannel));
    }

    @Test
    @DisplayName("SUBSCRIBE to DM conversation is restricted to conversation participants")
    void subscribeDmRequiresConversationMembership() {
        TenantRecord tenant = provisionTenant("ws-dm-membership");
        User alice = createUser(tenant, "alice@dm.test", Role.USER);
        User bob = createUser(tenant, "bob@dm.test", Role.USER);
        User eve = createUser(tenant, "eve@dm.test", Role.USER);

        // Create DM between Alice and Bob
        DirectConversation convo;
        TenantContext.setTenant(tenant.schemaName());
        try {
            convo = directConversationRepository.save(DirectConversation.between(alice.getId(), bob.getId()));
        } finally {
            TenantContext.clear();
        }

        String dmDestination = "/topic/tenant/" + tenant.tenantId() + "/dm/" + convo.getId();

        // Alice subscribes -> succeeds
        Message<?> aliceSub = createSubscribeMessage(dmDestination, tenant.tenantId(), tenant.schemaName(), alice.getId().toString());
        assertDoesNotThrow(() -> interceptor.preSend(aliceSub, dummyChannel));

        // Bob subscribes -> succeeds
        Message<?> bobSub = createSubscribeMessage(dmDestination, tenant.tenantId(), tenant.schemaName(), bob.getId().toString());
        assertDoesNotThrow(() -> interceptor.preSend(bobSub, dummyChannel));

        // Eve (not in conversation) subscribes -> blocked
        Message<?> eveSub = createSubscribeMessage(dmDestination, tenant.tenantId(), tenant.schemaName(), eve.getId().toString());
        SecurityException ex = assertThrows(SecurityException.class,
                () -> interceptor.preSend(eveSub, dummyChannel));
        assertEquals("Unauthorized direct conversation access", ex.getMessage());
    }

    @Test
    @DisplayName("Concurrent WebSocket connection and subscription load resilience")
    void concurrentConnectionsAndSubscriptionsResilience() throws InterruptedException {
        TenantRecord tenant = provisionTenant("ws-resilience");
        int threadCount = 10;
        int operationsPerThread = 20;

        List<User> users = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User u = createUser(tenant, "load-user-" + i + "@resilience.test", Role.USER);
            users.add(u);
            tokens.add(issueValidToken(u, tenant.tenantId()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int userIdx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    User user = users.get(userIdx);
                    String token = tokens.get(userIdx);

                    for (int op = 0; op < operationsPerThread; op++) {
                        // 1. Concurrent CONNECT
                        Message<?> connectMsg = createConnectMessage(token);
                        Message<?> connResult = interceptor.preSend(connectMsg, dummyChannel);
                        assertNotNull(connResult);

                        // 2. Concurrent SUBSCRIBE to tenant topic
                        String chanDest = "/topic/tenant/" + tenant.tenantId() + "/channels/" + UUID.randomUUID();
                        Message<?> chanSub = createSubscribeMessage(chanDest, tenant.tenantId(), tenant.schemaName(), user.getId().toString());
                        interceptor.preSend(chanSub, dummyChannel);

                        // 3. Concurrent SUBSCRIBE to user notification
                        String notifDest = "/topic/tenant/" + tenant.tenantId() + "/users/" + user.getId() + "/notifications";
                        Message<?> notifSub = createSubscribeMessage(notifDest, tenant.tenantId(), tenant.schemaName(), user.getId().toString());
                        interceptor.preSend(notifSub, dummyChannel);
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent WebSocket operations timed out");
        assertTrue(errors.isEmpty(), "Concurrent WebSocket operations generated errors: " + errors);
    }
}
