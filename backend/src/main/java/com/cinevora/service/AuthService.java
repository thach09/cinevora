package com.cinevora.service;

import com.cinevora.dto.AuthDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.entity.*;
import com.cinevora.exception.BusinessException;
import com.cinevora.repository.UserRepository;
import com.cinevora.repository.PasswordResetTokenRepository;
import com.cinevora.repository.ProfileRepository;
import com.cinevora.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    @org.springframework.beans.factory.annotation.Value("${app.auth.block-demo-identities:false}") private boolean blockDemo;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SessionService sessions;
    private final PasswordResetTokenRepository resetTokens;
    private final ProfileRepository profiles;
    private final SensitiveActionRateLimiter limiter;
    private final boolean exposeDevelopmentTokens;
    @Autowired
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, SessionService sessions,
                       PasswordResetTokenRepository resetTokens, ProfileRepository profiles, SensitiveActionRateLimiter limiter,
                       @Value("${app.auth.expose-development-tokens:false}") boolean exposeDevelopmentTokens) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.sessions = sessions; this.resetTokens = resetTokens; this.profiles = profiles; this.limiter = limiter; this.exposeDevelopmentTokens = exposeDevelopmentTokens;
    }
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this(users, encoder, jwt, null, null, null, new SensitiveActionRateLimiter(), false);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, String userAgent, String ipAddress) {
        String key = "login:" + request.username().trim().toLowerCase();
        if (!limiter.allow(key)) throw new com.cinevora.exception.RateLimitException();
        User user = users.findByUsernameIgnoreCase(request.username().trim()).orElse(null);
        if (user == null || (blockDemo && user.getId() != null && user.getId() <= 11) || !user.isActive() || !encoder.matches(request.password(), user.getPassword()))
            throw new BusinessException("Sai tên đăng nhập hoặc mật khẩu!");
        limiter.reset(key);
        return issue(user, userAgent, ipAddress, null);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) { return login(request, null, null); }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request, String userAgent, String ipAddress) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new BusinessException("Tên đăng nhập đã được sử dụng");
        if (users.existsByEmailIgnoreCase(email)) throw new BusinessException("Email đã được sử dụng");
        User user = new User();
        user.setUsername(username); user.setEmail(email); user.setPassword(encoder.encode(request.password()));
        user.setFullName(request.fullName().trim()); user.setRole(Role.CUSTOMER); user.setActive(true);
        String verificationToken = TokenService.randomToken();
        user.setEmailVerificationTokenHash(TokenService.sha256(verificationToken));
        user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        user = users.save(user);
        profiles.save(new Profile(user, user.getFullName(), true));
        return issue(user, userAgent, ipAddress, exposeDevelopmentTokens ? verificationToken : null);
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) { return register(request, null, null); }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request, String userAgent, String ipAddress) {
        SessionService.Rotation rotation = sessions.consume(request.refreshToken(), userAgent, ipAddress);
        User user = rotation.user();
        String nextToken = rotation.token();
        return new AuthDtos.AuthResponse(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), UserResponse.from(user), nextToken, user.isEmailVerified(), null);
    }

    @Transactional
    public void logout(AuthDtos.LogoutRequest request) { sessions.revoke(request.refreshToken()); }

    @Transactional
    public AuthDtos.GenericTokenResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String key = "reset:" + request.email().trim().toLowerCase();
        if (!limiter.allow(key)) throw new com.cinevora.exception.RateLimitException();
        String developmentToken = null;
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user != null && user.isActive()) {
            developmentToken = TokenService.randomToken();
            resetTokens.save(new PasswordResetToken(user, TokenService.sha256(developmentToken), Instant.now().plus(30, ChronoUnit.MINUTES)));
        }
        return new AuthDtos.GenericTokenResponse("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi.", exposeDevelopmentTokens ? developmentToken : null);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        PasswordResetToken reset = resetTokens.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(TokenService.sha256(request.token()), Instant.now())
                .orElseThrow(() -> new BusinessException("Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));
        User user = reset.getUser();
        user.setPassword(encoder.encode(request.newPassword()));
        user.invalidateCredentials();
        reset.markUsed();
        sessions.revokeAll(user);
        limiter.reset("reset:" + user.getEmail().toLowerCase());
    }

    @Transactional
    public void verifyEmail(AuthDtos.VerifyEmailRequest request) {
        User user = users.findByEmailVerificationTokenHash(TokenService.sha256(request.token()))
                .orElseThrow(() -> new BusinessException("Mã xác minh email không hợp lệ"));
        if (user.getEmailVerificationExpiresAt() == null || user.getEmailVerificationExpiresAt().isBefore(Instant.now())) throw new BusinessException("Mã xác minh email đã hết hạn");
        user.setEmailVerified(true); user.setEmailVerificationTokenHash(null); user.setEmailVerificationExpiresAt(null);
    }

    private AuthDtos.AuthResponse issue(User user, String userAgent, String ipAddress, String emailVerificationToken) {
        String refreshToken = sessions.create(user, userAgent, ipAddress);
        return new AuthDtos.AuthResponse(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), UserResponse.from(user), refreshToken, user.isEmailVerified(), emailVerificationToken);
    }
}
