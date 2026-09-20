package com.yourco.saas.collaboration.controller;

import com.yourco.saas.billing.RequiresFeature;
import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.collaboration.service.ChatService;
import com.yourco.saas.domain.billing.Feature;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiresFeature(Feature.TEAM_CHAT)
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ==========================================
    // CHANNELS
    // ==========================================

    @GetMapping("/channels")
    public ResponseEntity<List<ChatChannelResponse>> getChannels() {
        return ResponseEntity.ok(chatService.getChannels());
    }

    @PostMapping("/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ChatChannelResponse> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        ChatChannelResponse response = chatService.createChannel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/channels/{channelId}")
    public ResponseEntity<ChatChannelResponse> getChannelById(@PathVariable UUID channelId) {
        return ResponseEntity.ok(chatService.getChannelById(channelId));
    }

    @DeleteMapping("/channels/{channelId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID channelId) {
        chatService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/channel")
    public ResponseEntity<ChatChannelResponse> getOrCreateProjectChannel(@PathVariable UUID projectId) {
        return ResponseEntity.ok(chatService.getOrCreateProjectChannel(projectId));
    }

    // ==========================================
    // CHANNEL MESSAGES
    // ==========================================

    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getChannelMessages(
            @PathVariable UUID channelId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(chatService.getChannelMessages(channelId, pageable));
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ChatMessageResponse> sendChannelMessage(
            @PathVariable UUID channelId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = chatService.sendChannelMessage(channelId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // DIRECT CONVERSATIONS & MESSAGES
    // ==========================================

    @GetMapping("/direct")
    public ResponseEntity<List<DirectConversationResponse>> getDirectConversations() {
        return ResponseEntity.ok(chatService.getDirectConversations());
    }

    @PostMapping("/direct/{userId}")
    public ResponseEntity<DirectConversationResponse> getOrCreateDirectConversation(@PathVariable UUID userId) {
        DirectConversationResponse response = chatService.getOrCreateDirectConversation(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/direct/{conversationId}/messages")
    public ResponseEntity<Page<DirectMessageResponse>> getDirectMessages(
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(chatService.getDirectMessages(conversationId, pageable));
    }

    @PostMapping("/direct/{conversationId}/messages")
    public ResponseEntity<DirectMessageResponse> sendDirectMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        DirectMessageResponse response = chatService.sendDirectMessage(conversationId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
