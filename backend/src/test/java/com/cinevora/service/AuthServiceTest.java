package com.cinevora.service;

import com.cinevora.dto.AuthDtos;
import com.cinevora.entity.Role;
import com.cinevora.entity.User;
import com.cinevora.exception.BusinessException;
import com.cinevora.repository.UserRepository;
import com.cinevora.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock JwtService jwt;

    @Test void loginRejectsInvalidCredentialsWithoutRevealingWhichFieldFailed() {
        User user = new User(); user.setUsername("admin"); user.setPassword(new BCryptPasswordEncoder().encode("correct-password")); user.setRole(Role.ADMIN); user.setActive(true);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(users, new BCryptPasswordEncoder(), jwt);
        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class, () -> service.login(new AuthDtos.LoginRequest("admin", "wrong-password")));
        org.junit.jupiter.api.Assertions.assertEquals("Sai tên đăng nhập hoặc mật khẩu!", ex.getMessage());
        verifyNoInteractions(jwt);
    }
}
