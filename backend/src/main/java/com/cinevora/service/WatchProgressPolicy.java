package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import org.springframework.stereotype.Component;

/** Centralized playback rules. Keeping these thresholds here prevents drift between API and player. */
@Component
public class WatchProgressPolicy {
    public static final int MIN_CONTINUE_PERCENT = 2;
    public static final int COMPLETION_PERCENT = 90;

    public int normalizePercent(Integer percent) {
        if (percent == null) return 0;
        if (percent < 0 || percent > 100) throw new BusinessException("Tiến độ phải nằm trong khoảng 0 đến 100%");
        return percent;
    }

    public int resolveDuration(Integer requestedDurationSeconds, Integer movieDurationMinutes) {
        if (requestedDurationSeconds != null && requestedDurationSeconds > 0) return requestedDurationSeconds;
        if (movieDurationMinutes != null && movieDurationMinutes > 0) return movieDurationMinutes * 60;
        return 0;
    }

    public int resolvePosition(Integer requestedPositionSeconds, int durationSeconds, int percent) {
        int position = requestedPositionSeconds == null
                ? (durationSeconds > 0 ? Math.round((percent / 100f) * durationSeconds) : 0)
                : requestedPositionSeconds;
        if (position < 0) throw new BusinessException("Vị trí phát không hợp lệ");
        return durationSeconds > 0 ? Math.min(position, durationSeconds) : position;
    }

    public int derivePercent(int positionSeconds, int durationSeconds, int fallbackPercent) {
        if (durationSeconds <= 0) return normalizePercent(fallbackPercent);
        return Math.max(0, Math.min(100, Math.round((positionSeconds * 100f) / durationSeconds)));
    }

    public boolean isComplete(int percent, int positionSeconds, int durationSeconds) {
        return percent >= COMPLETION_PERCENT || (durationSeconds > 0 && positionSeconds >= Math.ceil(durationSeconds * COMPLETION_PERCENT / 100d));
    }

    public boolean shouldContinue(int percent, int positionSeconds, int durationSeconds) {
        return percent >= MIN_CONTINUE_PERCENT && !isComplete(percent, positionSeconds, durationSeconds);
    }
}
