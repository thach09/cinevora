package com.cinevora.service;
import java.net.URI;
import com.cinevora.exception.BusinessException;
public final class MediaUrlPolicy {
    private MediaUrlPolicy() {}
    public static String validate(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        try {
            URI uri = URI.create(clean);
            if (clean.startsWith("/media/") && !clean.contains("\\") && !clean.contains("..")) return clean;
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) return clean;
        } catch (IllegalArgumentException ignored) { }
        throw new BusinessException("Media URL must be HTTP(S) or a local media path");
    }
}
