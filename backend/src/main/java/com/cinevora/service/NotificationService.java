package com.cinevora.service;

import com.cinevora.dto.NotificationDtos;
import com.cinevora.entity.Notification;
import com.cinevora.entity.User;
import com.cinevora.entity.Role;
import com.cinevora.exception.BusinessException;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.NotificationRepository;
import com.cinevora.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(NotificationRepository notifications, UserRepository users) { this.notifications = notifications; this.users = users; }
    @Transactional(readOnly = true) public NotificationDtos.Inbox inbox(String username) { User user = user(username); return new NotificationDtos.Inbox(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(NotificationDtos.Response::from).toList(), notifications.countByUser_IdAndReadAtIsNull(user.getId())); }
    @Transactional public void markRead(String username, Long id) { Notification notification = notifications.findByIdAndUser_Id(id, user(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification " + id)); notification.markRead(); }
    @Transactional public void markAllRead(String username) { notifications.findByUser_IdAndReadAtIsNull(user(username).getId()).forEach(Notification::markRead); }
    @Transactional public void create(User user, String kind, String title, String body, String actionUrl) { notifications.save(new Notification(user, kind, title, body, safeActionUrl(actionUrl))); }
    @Transactional public NotificationDtos.DispatchResponse sendAdminMessage(NotificationDtos.AdminMessageRequest request) {
        String title = requiredText(request.title(), "Notification title", 160);
        String body = requiredText(request.body(), "Notification body", 4000);
        String actionUrl = safeActionUrl(request.actionUrl());
        java.util.List<User> recipients;
        if (request.broadcastToActiveCustomers()) {
            recipients = users.findByRoleAndActiveTrue(Role.CUSTOMER);
        } else {
            User recipient = users.findByUsernameIgnoreCase(request.recipientUsername().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            if (recipient.getRole() != Role.CUSTOMER || !recipient.isActive())
                throw new BusinessException("Notification recipients must be active customer accounts");
            recipients = java.util.List.of(recipient);
        }
        notifications.saveAll(recipients.stream()
                .map(user -> new Notification(user, "ADMIN_MESSAGE", title, body, actionUrl)).toList());
        return new NotificationDtos.DispatchResponse(recipients.size());
    }
    private String safeActionUrl(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) return null;
        String value = actionUrl.trim();
        if (value.length() > 500 || value.indexOf('\u0000') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new IllegalArgumentException("Notification action URL is invalid");
        try {
            URI uri = new URI(value);
            if (!value.startsWith("/") || value.startsWith("//") || uri.isAbsolute() || uri.getFragment() != null)
                throw new IllegalArgumentException("Notification action URL must be a relative application path");
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Notification action URL is invalid", ex);
        }
        return value;
    }
    private String requiredText(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.trim().length() > maximum)
            throw new BusinessException(field + " is invalid");
        return value.trim();
    }
    private User user(String username) { return users.findByUsernameIgnoreCase(username).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user")); }
}
