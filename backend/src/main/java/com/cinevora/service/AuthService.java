package com.cinevora.service;

import com.cinevora.dto.AuthDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.entity.*;
import com.cinevora.exception.BusinessException;
import com.cinevora.repository.UserRepository;
import com.cinevora.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByUsernameIgnoreCase(request.username().trim()).orElse(null);
        if (user == null || !user.isActive() || !encoder.matches(request.password(), user.getPassword()))
            throw new BusinessException("Sai tên đăng nhập hoặc mật khẩu!");
        return new AuthDtos.AuthResponse(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), UserResponse.from(user));
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new BusinessException("Tên đăng nhập đã được sử dụng");
        if (users.existsByEmailIgnoreCase(email)) throw new BusinessException("Email đã được sử dụng");
        User user = new User();
        user.setUsername(username); user.setEmail(email); user.setPassword(encoder.encode(request.password()));
        user.setFullName(request.fullName().trim()); user.setRole(Role.CUSTOMER); user.setActive(true);
        user = users.save(user);
        return new AuthDtos.AuthResponse(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), UserResponse.from(user));
    }
}
