package com.cinevora.service;

import com.cinevora.dto.NotificationDtos;
import com.cinevora.entity.Notification;
import com.cinevora.entity.User;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.NotificationRepository;
import com.cinevora.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(NotificationRepository notifications, UserRepository users) { this.notifications = notifications; this.users = users; }
    @Transactional(readOnly = true) public NotificationDtos.Inbox inbox(String username) { User user = user(username); return new NotificationDtos.Inbox(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(NotificationDtos.Response::from).toList(), notifications.countByUser_IdAndReadAtIsNull(user.getId())); }
    @Transactional public void markRead(String username, Long id) { Notification notification = notifications.findByIdAndUser_Id(id, user(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification " + id)); notification.markRead(); }
    @Transactional public void markAllRead(String username) { notifications.findByUser_IdAndReadAtIsNull(user(username).getId()).forEach(Notification::markRead); }
    @Transactional public void create(User user, String kind, String title, String body, String actionUrl) { notifications.save(new Notification(user, kind, title, body, actionUrl)); }
    private User user(String username) { return users.findByUsernameIgnoreCase(username).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user")); }
}
