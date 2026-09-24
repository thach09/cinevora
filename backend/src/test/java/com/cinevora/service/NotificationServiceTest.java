package com.cinevora.service;

import com.cinevora.entity.User;
import com.cinevora.entity.Role;
import com.cinevora.dto.NotificationDtos;
import com.cinevora.exception.BusinessException;
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
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void broadcastSendsOnlyToActiveCustomers() {
        NotificationService service = new NotificationService(notifications, users);
        User customer = new User(); customer.setRole(Role.CUSTOMER); customer.setActive(true);
        when(users.findByRoleAndActiveTrue(Role.CUSTOMER)).thenReturn(java.util.List.of(customer));

        var result = service.sendAdminMessage(new NotificationDtos.AdminMessageRequest(null, true, "New release", "A title was added.", "/browse"));

        assertEquals(1, result.recipientCount());
        verify(notifications).saveAll(any());
    }

    @Test
    void individualRecipientMustBeAnActiveCustomer() {
        NotificationService service = new NotificationService(notifications, users);
        User admin = new User(); admin.setRole(Role.ADMIN); admin.setActive(true);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(java.util.Optional.of(admin));

        assertThrows(BusinessException.class, () -> service.sendAdminMessage(
                new NotificationDtos.AdminMessageRequest("admin", false, "Title", "Body", null)));
        verifyNoInteractions(notifications);
    }

    @Test
    void adminMessageRejectsMaliciousActionUrlBeforeDelivery() {
        NotificationService service = new NotificationService(notifications, users);

        assertThrows(IllegalArgumentException.class, () -> service.sendAdminMessage(
                new NotificationDtos.AdminMessageRequest(null, true, "Title", "Body", "javascript:alert(1)")));
        verifyNoInteractions(notifications);
    }
}
