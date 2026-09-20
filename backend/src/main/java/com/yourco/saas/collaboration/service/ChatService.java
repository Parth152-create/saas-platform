package com.yourco.saas.collaboration.service;

import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.collaboration.websocket.WebSocketEventBroadcaster;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.collaboration.*;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectMemberRepository;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatChannelRepository channelRepository;
    private final ChatChannelMemberRepository channelMemberRepository;
    private final ChatMessageRepository messageRepository;
    private final DirectConversationRepository directConversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final WebSocketEventBroadcaster webSocketEventBroadcaster;
    private final TenantRegistryService tenantRegistryService;

    public ChatService(ChatChannelRepository channelRepository,
                       ChatChannelMemberRepository channelMemberRepository,
                       ChatMessageRepository messageRepository,
                       DirectConversationRepository directConversationRepository,
                       DirectMessageRepository directMessageRepository,
                       UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository,
                       AuditLogRepository auditLogRepository,
                       NotificationService notificationService,
                       WebSocketEventBroadcaster webSocketEventBroadcaster,
                       TenantRegistryService tenantRegistryService) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.messageRepository = messageRepository;
        this.directConversationRepository = directConversationRepository;
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.webSocketEventBroadcaster = webSocketEventBroadcaster;
        this.tenantRegistryService = tenantRegistryService;
    }

    // ==========================================
    // CHANNELS
    // ==========================================

    public List<ChatChannelResponse> getChannels() {
        ensureDefaultWorkspaceChannels();

        List<ChatChannel> allChannels = channelRepository.findAll();
        List<ChatChannelResponse> responses = new ArrayList<>();

        for (ChatChannel channel : allChannels) {
            Optional<ChatMessage> lastMsg = messageRepository.findFirstByChannelIdOrderByCreatedAtDesc(channel.getId());
            String lastContent = lastMsg.map(ChatMessage::getContent).orElse(null);
            Instant lastAt = lastMsg.map(ChatMessage::getCreatedAt).orElse(channel.getCreatedAt());

            responses.add(ChatChannelResponse.fromEntity(channel, 0, lastContent, lastAt));
        }

        return responses;
    }

    public ChatChannelResponse getOrCreateProjectChannel(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        ChatChannel channel = channelRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    ChatChannel newCh = new ChatChannel(
                            "project-" + project.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"),
                            "Channel for project: " + project.getName(),
                            ChannelType.PROJECT,
                            projectId,
                            currentActorId()
                    );
                    return channelRepository.save(newCh);
                });

        Optional<ChatMessage> lastMsg = messageRepository.findFirstByChannelIdOrderByCreatedAtDesc(channel.getId());
        String lastContent = lastMsg.map(ChatMessage::getContent).orElse(null);
        Instant lastAt = lastMsg.map(ChatMessage::getCreatedAt).orElse(channel.getCreatedAt());

        return ChatChannelResponse.fromEntity(channel, 0, lastContent, lastAt);
    }

    public ChatChannelResponse createChannel(CreateChannelRequest request) {
        validateManagerOrAbove();

        if (request.type() == ChannelType.PROJECT && request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            Optional<ChatChannel> existing = channelRepository.findByProjectId(request.projectId());
            if (existing.isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project channel already exists for this project");
            }
        }

        ChatChannel channel = new ChatChannel(
                request.name().trim(),
                request.description(),
                request.type() != null ? request.type() : ChannelType.WORKSPACE,
                request.projectId(),
                currentActorId()
        );
        ChatChannel saved = channelRepository.save(channel);

        auditLogRepository.save(AuditLog.of(
                currentActorId(),
                "CHANNEL_CREATED",
                "ChatChannel",
                saved.getId().toString(),
                "name=" + saved.getName() + ",type=" + saved.getType()
        ));

        return ChatChannelResponse.fromEntity(saved, 0, null, saved.getCreatedAt());
    }

    public ChatChannelResponse getChannelById(UUID channelId) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        Optional<ChatMessage> lastMsg = messageRepository.findFirstByChannelIdOrderByCreatedAtDesc(channel.getId());
        String lastContent = lastMsg.map(ChatMessage::getContent).orElse(null);
        Instant lastAt = lastMsg.map(ChatMessage::getCreatedAt).orElse(channel.getCreatedAt());

        return ChatChannelResponse.fromEntity(channel, 0, lastContent, lastAt);
    }

    public void deleteChannel(UUID channelId) {
        validateAdminOrAbove();

        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        channelRepository.delete(channel);

        auditLogRepository.save(AuditLog.of(
                currentActorId(),
                "CHANNEL_DELETED",
                "ChatChannel",
                channelId.toString(),
                "name=" + channel.getName()
        ));
    }

    // ==========================================
    // CHANNEL MESSAGES
    // ==========================================

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getChannelMessages(UUID channelId, Pageable pageable) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        return messageRepository.findByChannelIdOrderByCreatedAtDesc(channel.getId(), pageable)
                .map(msg -> {
                    ResolvedUser user = resolveUser(msg.getSenderId());
                    return ChatMessageResponse.fromEntity(msg, user.name(), user.email());
                });
    }

    public ChatMessageResponse sendChannelMessage(UUID channelId, String content) {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        validateUserActive(actorId);

        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be blank");
        }
        if (content.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum length of 4000 characters");
        }

        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));

        ChatMessage message = new ChatMessage(channel.getId(), actorId, content.trim());
        ChatMessage saved = messageRepository.save(message);

        ResolvedUser sender = resolveUser(actorId);
        ChatMessageResponse response = ChatMessageResponse.fromEntity(saved, sender.name(), sender.email());

        // Process @mentions
        processMentions(content, channel, sender.name());

        // Broadcast via WebSocket
        String tenantId = currentTenantId();
        if (tenantId != null) {
            try {
                webSocketEventBroadcaster.broadcastChannelMessage(tenantId, channel.getId(), response);
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket channel message: {}", e.getMessage());
            }
        }

        return response;
    }

    // ==========================================
    // DIRECT CONVERSATIONS & DMs
    // ==========================================

    public List<DirectConversationResponse> getDirectConversations() {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        List<DirectConversation> convos = directConversationRepository.findForUser(currentUserId);
        List<DirectConversationResponse> result = new ArrayList<>();

        for (DirectConversation convo : convos) {
            UUID otherUserId = convo.getOtherUser(currentUserId);
            ResolvedUser other = resolveUser(otherUserId);
            User otherUserEntity = userRepository.findById(otherUserId).orElse(null);

            Optional<DirectMessage> lastMsg = directMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(convo.getId());
            String lastContent = lastMsg.map(DirectMessage::getContent).orElse(null);
            Instant lastAt = lastMsg.map(DirectMessage::getCreatedAt).orElse(convo.getCreatedAt());
            long unread = directMessageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(convo.getId(), currentUserId);

            result.add(new DirectConversationResponse(
                    convo.getId(),
                    otherUserId,
                    other.name(),
                    other.email(),
                    otherUserEntity != null ? otherUserEntity.getRole().name() : "USER",
                    lastContent,
                    lastAt,
                    unread,
                    convo.getCreatedAt()
            ));
        }

        return result;
    }

    public DirectConversationResponse getOrCreateDirectConversation(UUID targetUserId) {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        validateUserActive(currentUserId);

        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot open direct conversation with yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message an inactive user");
        }

        DirectConversation convo = directConversationRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseGet(() -> directConversationRepository.save(DirectConversation.between(currentUserId, targetUserId)));

        ResolvedUser other = resolveUser(targetUserId);
        Optional<DirectMessage> lastMsg = directMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(convo.getId());
        String lastContent = lastMsg.map(DirectMessage::getContent).orElse(null);
        Instant lastAt = lastMsg.map(DirectMessage::getCreatedAt).orElse(convo.getCreatedAt());
        long unread = directMessageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(convo.getId(), currentUserId);

        return new DirectConversationResponse(
                convo.getId(),
                targetUserId,
                other.name(),
                other.email(),
                targetUser.getRole().name(),
                lastContent,
                lastAt,
                unread,
                convo.getCreatedAt()
        );
    }

    @Transactional
    public Page<DirectMessageResponse> getDirectMessages(UUID conversationId, Pageable pageable) {
        UUID currentUserId = currentActorId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        DirectConversation convo = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!convo.includesUser(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to conversation");
        }

        // Mark incoming messages as read
        directMessageRepository.markAllReadForConversation(conversationId, currentUserId, Instant.now());

        return directMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(msg -> {
                    ResolvedUser sender = resolveUser(msg.getSenderId());
                    return DirectMessageResponse.fromEntity(msg, sender.name(), sender.email());
                });
    }

    public DirectMessageResponse sendDirectMessage(UUID conversationId, String content) {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        validateUserActive(actorId);

        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be blank");
        }
        if (content.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum length of 4000 characters");
        }

        DirectConversation convo = directConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!convo.includesUser(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to conversation");
        }

        UUID recipientId = convo.getOtherUser(actorId);
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient user is not active");
        }

        DirectMessage msg = new DirectMessage(conversationId, actorId, content.trim());
        DirectMessage saved = directMessageRepository.save(msg);

        // Update conversation timestamp
        convo.includesUser(actorId); // touch entity
        directConversationRepository.save(convo);

        ResolvedUser sender = resolveUser(actorId);
        DirectMessageResponse response = DirectMessageResponse.fromEntity(saved, sender.name(), sender.email());

        // Send notification to recipient
        notificationService.createNotification(
                recipientId,
                NotificationType.DIRECT_MESSAGE,
                "New message from " + sender.name(),
                content.length() > 80 ? content.substring(0, 77) + "..." : content,
                "DM",
                conversationId.toString(),
                "/app/chat"
        );

        // Broadcast via WebSocket
        String tenantId = currentTenantId();
        if (tenantId != null) {
            try {
                webSocketEventBroadcaster.broadcastDirectMessage(tenantId, conversationId, response);
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket direct message: {}", e.getMessage());
            }
        }

        return response;
    }

    // ==========================================
    // HELPERS & MENTIONS
    // ==========================================

    private void ensureDefaultWorkspaceChannels() {
        List<ChatChannel> channels = channelRepository.findByTypeOrderByCreatedAtAsc(ChannelType.WORKSPACE);
        if (channels.isEmpty()) {
            channelRepository.save(new ChatChannel(
                    "general",
                    "Company-wide announcements and general workplace discussion",
                    ChannelType.WORKSPACE,
                    null,
                    null
            ));
            channelRepository.save(new ChatChannel(
                    "random",
                    "Casual conversations and non-work chatter",
                    ChannelType.WORKSPACE,
                    null,
                    null
            ));
        }
    }

    private void processMentions(String content, ChatChannel channel, String senderName) {
        Pattern mentionPattern = Pattern.compile("@([a-zA-Z0-9._-]+)");
        Matcher matcher = mentionPattern.matcher(content);
        Set<String> mentionedHandles = new HashSet<>();

        while (matcher.find()) {
            mentionedHandles.add(matcher.group(1).toLowerCase());
        }

        if (mentionedHandles.isEmpty()) return;

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            String emailPrefix = user.getEmail().split("@")[0].toLowerCase();
            if (mentionedHandles.contains(emailPrefix)) {
                notificationService.createNotification(
                        user.getId(),
                        NotificationType.CHAT_MENTION,
                        senderName + " mentioned you in #" + channel.getName(),
                        content.length() > 80 ? content.substring(0, 77) + "..." : content,
                        "CHAT_CHANNEL",
                        channel.getId().toString(),
                        "/app/chat"
                );
            }
        }
    }

    public record ResolvedUser(String name, String email) {}

    public ResolvedUser resolveUser(UUID id) {
        if (id == null) return new ResolvedUser("System", "");
        Optional<Employee> emp = employeeRepository.findById(id);
        if (emp.isPresent()) {
            return new ResolvedUser(emp.get().getName(), emp.get().getEmail());
        }
        Optional<User> u = userRepository.findById(id);
        if (u.isPresent()) {
            String email = u.get().getEmail();
            String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            return new ResolvedUser(name, email);
        }
        return new ResolvedUser("User", "");
    }

    private void validateUserActive(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disabled users cannot send messages");
        }
    }

    private void validateManagerOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires MANAGER or above");
        }
    }

    private void validateAdminOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires ADMIN or above");
        }
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

    private Role currentActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if (authority.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(authority.substring(5));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    private String currentTenantId() {
        String schema = TenantContext.getTenant();
        if (schema == null) return null;
        return tenantRegistryService.findBySchemaName(schema)
                .map(TenantRecord::tenantId)
                .orElse(null);
    }
}
