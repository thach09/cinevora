package com.cinevora.service;

import com.cinevora.entity.User;
import com.cinevora.repository.NotificationRepository;
import com.cinevora.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock UserRepository users;

    @Test
    void createRejectsExternalNotificationNavigation() {
        NotificationService service = new NotificationService(notifications, users);
        User user = new User();

        assertThrows(IllegalArgumentException.class, () -> service.create(user, "SYSTEM", "Title", "Body", "https://evil.example"));
        verifyNoInteractions(notifications);
    }

    @Test
    void createAcceptsRelativeApplicationNavigation() {
        NotificationService service = new NotificationService(notifications, users);
        service.create(new User(), "SYSTEM", "Title", "Body", "/browse?source=notification");

        verify(notifications).save(any());
    }
}
