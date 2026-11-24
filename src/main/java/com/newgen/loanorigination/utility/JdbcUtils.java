package com.newgen.loanorigination.utility;

import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

public final class JdbcUtils {

    private JdbcUtils() {}

    public static Long extractGeneratedKey(KeyHolder keyHolder, String columnName) {
        if (keyHolder == null) {
            return null;
        }
        try {
            if (keyHolder.getKey() != null) {
                return keyHolder.getKey().longValue();
            }
        } catch (Exception ignored) {
            // Multi-column generated keys returned by database driver (e.g. H2 returning default timestamps)
        }
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            Map<String, Object> keys = keyHolder.getKeys();
            for (Map.Entry<String, Object> entry : keys.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(columnName) && entry.getValue() instanceof Number) {
                    return ((Number) entry.getValue()).longValue();
                }
            }
            for (Object val : keys.values()) {
                if (val instanceof Number) {
                    return ((Number) val).longValue();
                }
            }
        }
        return null;
    }
}
