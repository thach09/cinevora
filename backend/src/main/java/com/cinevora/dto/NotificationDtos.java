package com.cinevora.dto;

import com.cinevora.entity.Notification;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record Response(Long id, String kind, String title, String body, String actionUrl, boolean read, Instant createdAt) {
        public static Response from(Notification notification) { return new Response(notification.getId(), notification.getKind(), notification.getTitle(), notification.getBody(), notification.getActionUrl(), notification.getReadAt() != null, notification.getCreatedAt()); }
    }
    public record Inbox(java.util.List<Response> items, long unreadCount) {}
}
