package me.lokspel.spawnauth.storage;

import java.util.Locale;

public enum DatabaseType {
    SQLITE,
    MYSQL,
    NONE;

    public static DatabaseType fromString(String value) {
        if (value == null) {
            return SQLITE;
        }
        try {
            return DatabaseType.valueOf(value.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException exception) {
            return SQLITE;
        }
    }
}
