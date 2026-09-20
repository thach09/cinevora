package com.cinevora.dto;

import com.cinevora.entity.Profile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {
    private ProfileDtos() {}
    public record Request(@NotBlank @Size(max = 80) String name, @Size(max = 500) String avatarUrl) {}
    public record Response(Long id, String name, String avatarUrl, boolean defaultProfile) {
        public static Response from(Profile profile) { return new Response(profile.getId(), profile.getName(), profile.getAvatarUrl(), profile.isDefaultProfile()); }
    }
}
