package me.lokspel.spawnauth.config;

import java.util.Locale;

public enum SpawnMode {
    VANILLA,
    FIXED,
    DISABLED;

    public static SpawnMode fromString(String value) {
        if (value == null) {
            return VANILLA;
        }
        return switch (value.toLowerCase(Locale.ROOT).trim()) {
            case "fixed" -> FIXED;
            case "disabled" -> DISABLED;
            default -> VANILLA;
        };
    }
}
