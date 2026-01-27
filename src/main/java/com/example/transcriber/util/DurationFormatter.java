package com.example.audiototext.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DurationFormatter {

    public static String formatDuration(BigDecimal durationSeconds) {
        if (durationSeconds == null) {
            return "00:00";
        }

        long totalSeconds = durationSeconds.longValue();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
