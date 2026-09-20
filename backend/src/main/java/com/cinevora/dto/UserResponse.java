package com.cinevora.dto;

import com.cinevora.entity.User;

public record UserResponse(Long id, String username, String email, String fullName, String role, boolean active, boolean emailVerified) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole().name(), user.isActive(), user.isEmailVerified());
    }
}
