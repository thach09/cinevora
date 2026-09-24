package com.cinevora.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank @Size(max = 50) String username, @NotBlank @Size(min = 6, max = 72) String password) {}
    public record RegisterRequest(@NotBlank @Size(max = 50) String username,
                                  @NotBlank @Email @Size(max = 100) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(max = 100) String fullName) {}
    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user,
                               @com.fasterxml.jackson.annotation.JsonIgnore String refreshToken, boolean emailVerified, String emailVerificationToken) {}
    public record RefreshRequest(@NotBlank @Size(max = 256) String refreshToken) {}
    public record LogoutRequest(@Size(max = 256) String refreshToken) {}
    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 100) String email) {}
    public record ResetPasswordRequest(@NotBlank @Size(max = 256) String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record VerifyEmailRequest(@NotBlank @Size(max = 256) String token) {}
    public record GenericTokenResponse(String message, String developmentToken) {}
}
