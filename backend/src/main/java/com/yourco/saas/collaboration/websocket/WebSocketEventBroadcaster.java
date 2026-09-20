package com.yourco.saas.collaboration.websocket;

import com.yourco.saas.collaboration.dto.CalendarEventResponse;
import com.yourco.saas.collaboration.dto.ChatMessageResponse;
import com.yourco.saas.collaboration.dto.DirectMessageResponse;
import com.yourco.saas.collaboration.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebSocketEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastChannelMessage(String tenantId, UUID channelId, ChatMessageResponse message) {
        String destination = "/topic/tenant/" + tenantId + "/channels/" + channelId;
        log.debug("Broadcasting channel message to: {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void broadcastDirectMessage(String tenantId, UUID conversationId, DirectMessageResponse message) {
        String destination = "/topic/tenant/" + tenantId + "/dm/" + conversationId;
        log.debug("Broadcasting direct message to: {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendUserNotification(String tenantId, UUID userId, NotificationResponse notification) {
        String destination = "/topic/tenant/" + tenantId + "/users/" + userId + "/notifications";
        log.debug("Sending user notification to: {}", destination);
        messagingTemplate.convertAndSend(destination, notification);
    }

    public void broadcastCalendarEvent(String tenantId, CalendarEventResponse event) {
        String destination = "/topic/tenant/" + tenantId + "/calendar";
        log.debug("Broadcasting calendar event to: {}", destination);
        messagingTemplate.convertAndSend(destination, event);
    }
}
