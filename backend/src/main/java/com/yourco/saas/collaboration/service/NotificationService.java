package com.yourco.saas.collaboration.service;

import com.yourco.saas.collaboration.dto.NotificationResponse;
import com.yourco.saas.collaboration.dto.UnreadCountResponse;
import com.yourco.saas.collaboration.websocket.WebSocketEventBroadcaster;
import com.yourco.saas.domain.collaboration.Notification;
import com.yourco.saas.domain.collaboration.NotificationRepository;
import com.yourco.saas.domain.collaboration.NotificationType;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final WebSocketEventBroadcaster webSocketEventBroadcaster;
    private final TenantRegistryService tenantRegistryService;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               WebSocketEventBroadcaster webSocketEventBroadcaster,
                               TenantRegistryService tenantRegistryService,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.webSocketEventBroadcaster = webSocketEventBroadcaster;
        this.tenantRegistryService = tenantRegistryService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            return new UnreadCountResponse(0);
        }
        long count = notificationRepository.countByUserIdAndReadAtIsNull(currentUserId);
        return new UnreadCountResponse(count);
    }

    public NotificationResponse markAsRead(UUID id) {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    public void markAllAsRead() {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        notificationRepository.markAllAsReadForUser(currentUserId, Instant.now());
    }

    public NotificationResponse createNotification(UUID userId, NotificationType type, String title,
                                                   String message, String resourceType, String resourceId,
                                                   String targetPath) {
        if (userId == null || !userRepository.existsById(userId)) {
            log.debug("Skipping notification: target user {} does not exist in users table", userId);
            return null;
        }

        Notification notification = new Notification(userId, type, title, message, resourceType, resourceId, targetPath);
        Notification saved = notificationRepository.save(notification);

        NotificationResponse response = NotificationResponse.fromEntity(saved);

        // Deliver real-time notification if tenant is active
        String schema = TenantContext.getTenant();
        if (schema != null) {
            Optional<TenantRecord> tenantOpt = tenantRegistryService.findBySchemaName(schema);
            tenantOpt.ifPresent(tenant -> {
                try {
                    webSocketEventBroadcaster.sendUserNotification(tenant.tenantId(), userId, response);
                } catch (Exception e) {
                    log.warn("Failed to broadcast WebSocket notification to user {}: {}", userId, e.getMessage());
                }
            });
        }

        return response;
    }

    private UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
