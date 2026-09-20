package com.cinevora.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank String username, @NotBlank @Size(min = 6, max = 100) String password) {}
    public record RegisterRequest(@NotBlank @Size(max = 50) String username,
                                  @NotBlank @Email @Size(max = 100) String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 100) String fullName) {}
    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user,
                               String refreshToken, boolean emailVerified, String emailVerificationToken) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 100) String newPassword) {}
    public record VerifyEmailRequest(@NotBlank String token) {}
    public record GenericTokenResponse(String message, String developmentToken) {}
}
