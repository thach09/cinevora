package com.cinevora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AccountDtos {
    private AccountDtos() {}
    public record UpdateProfileRequest(@NotBlank @Size(max = 100) String fullName, @NotBlank @Email @Size(max = 100) String email) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {}
    public record SessionResponse(Long id, String userAgent, String ipAddress, java.time.Instant createdAt, java.time.Instant lastUsedAt, java.time.Instant expiresAt) {}
}
