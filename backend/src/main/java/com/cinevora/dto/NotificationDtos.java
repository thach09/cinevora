package com.cinevora.dto;

import com.cinevora.entity.Notification;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record Response(Long id, String kind, String title, String body, String actionUrl, boolean read, Instant createdAt) {
        public static Response from(Notification notification) { return new Response(notification.getId(), notification.getKind(), notification.getTitle(), notification.getBody(), notification.getActionUrl(), notification.getReadAt() != null, notification.getCreatedAt()); }
    }
    public record Inbox(java.util.List<Response> items, long unreadCount) {}
    public record AdminMessageRequest(
            @Size(max = 50) String recipientUsername,
            boolean broadcastToActiveCustomers,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String body,
            @Size(max = 500) String actionUrl) {
        @AssertTrue(message = "Choose one customer or all active customers")
        public boolean hasExactlyOneRecipientTarget() {
            return broadcastToActiveCustomers != (recipientUsername != null && !recipientUsername.isBlank());
        }
    }
    public record DispatchResponse(int recipientCount) {}
}
