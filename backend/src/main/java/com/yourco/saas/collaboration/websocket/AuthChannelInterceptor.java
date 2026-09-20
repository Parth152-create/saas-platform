package com.yourco.saas.collaboration.websocket;

import com.yourco.saas.auth.JwtService;
import com.yourco.saas.domain.collaboration.DirectConversation;
import com.yourco.saas.domain.collaboration.DirectConversationRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;
    private final TenantRegistryService tenantRegistryService;
    private final JwtService jwtService;
    private final DirectConversationRepository directConversationRepository;

    public AuthChannelInterceptor(JwtDecoder jwtDecoder,
                                  TenantRegistryService tenantRegistryService,
                                  JwtService jwtService,
                                  DirectConversationRepository directConversationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.tenantRegistryService = tenantRegistryService;
        this.jwtService = jwtService;
        this.directConversationRepository = directConversationRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected: Missing or invalid Authorization header");
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("Missing tenant_id claim in WebSocket token");
            }

            Optional<TenantRecord> tenantOpt = tenantRegistryService.findByTenantId(tenantId);
            if (tenantOpt.isEmpty() || !"ACTIVE".equalsIgnoreCase(tenantOpt.get().status())) {
                throw new IllegalArgumentException("Tenant is inactive or not found");
            }

            TenantRecord tenant = tenantOpt.get();
            String subject = jwt.getSubject();
            if (subject != null && jwtService != null) {
                try {
                    UUID userId = UUID.fromString(subject);
                    if (jwtService.isUserDisabled(userId)) {
                        throw new IllegalArgumentException("User is disabled");
                    }
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && e.getMessage().contains("User is disabled")) {
                        throw e;
                    }
                }
            }

            String role = jwt.getClaimAsString("role");
            List<GrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, null, authorities);

            accessor.setUser(authentication);

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("tenant_id", tenant.tenantId());
                sessionAttributes.put("tenant_schema", tenant.schemaName());
                sessionAttributes.put("user_id", subject);
            }

            log.info("WebSocket connected user: {} on tenant: {}", subject, tenantId);
        } catch (JwtException e) {
            log.warn("WebSocket CONNECT rejected due to JWT validation failure: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new SecurityException("Unauthorized WebSocket session");
        }

        String tenantId = (String) sessionAttributes.get("tenant_id");
        String tenantSchema = (String) sessionAttributes.get("tenant_schema");
        String userIdStr = (String) sessionAttributes.get("user_id");

        if (tenantId == null || tenantSchema == null || userIdStr == null) {
            throw new SecurityException("Incomplete WebSocket session credentials");
        }

        // Validate tenant isolation on all tenant-scoped topics: /topic/tenant/{targetTenantId}/...
        if (destination.startsWith("/topic/tenant/")) {
            String[] parts = destination.split("/");
            if (parts.length >= 4) {
                String targetTenantId = parts[3];
                if (!tenantId.equals(targetTenantId)) {
                    log.warn("Cross-tenant WebSocket subscription blocked: user tenant {} tried to subscribe to {}",
                            tenantId, destination);
                    throw new SecurityException("Cross-tenant subscription forbidden");
                }
            }
        }

        // Validate user isolation on user notifications: /topic/tenant/{tenantId}/users/{targetUserId}/...
        if (destination.contains("/users/")) {
            String[] parts = destination.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("users".equals(parts[i])) {
                    String targetUserId = parts[i + 1];
                    if (!userIdStr.equals(targetUserId)) {
                        log.warn("Cross-user WebSocket subscription blocked: user {} tried to subscribe to {}",
                                userIdStr, destination);
                        throw new SecurityException("Cross-user subscription forbidden");
                    }
                    break;
                }
            }
        }

        // Validate DM conversation membership: /topic/tenant/{tenantId}/dm/{conversationId}
        if (destination.contains("/dm/")) {
            String[] parts = destination.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("dm".equals(parts[i])) {
                    String convoIdStr = parts[i + 1];
                    try {
                        UUID convoId = UUID.fromString(convoIdStr);
                        UUID currentUserId = UUID.fromString(userIdStr);
                        try {
                            TenantContext.setTenant(tenantSchema);
                            Optional<DirectConversation> convo = directConversationRepository.findById(convoId);
                            if (convo.isEmpty() || !convo.get().includesUser(currentUserId)) {
                                log.warn("Unauthorized DM subscription: user {} not in conversation {}", currentUserId, convoId);
                                throw new SecurityException("Unauthorized direct conversation access");
                            }
                        } finally {
                            TenantContext.clear();
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid DM conversation UUID: {}", convoIdStr);
                    }
                    break;
                }
            }
        }
    }
}
