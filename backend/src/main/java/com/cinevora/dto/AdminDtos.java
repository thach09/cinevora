package com.cinevora.dto;

import jakarta.validation.constraints.NotNull;

public final class AdminDtos {
    private AdminDtos() {}
    public record ActiveRequest(@NotNull Boolean active) {}
}
