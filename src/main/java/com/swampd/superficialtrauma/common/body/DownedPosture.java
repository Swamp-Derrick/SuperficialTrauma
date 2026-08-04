package com.swampd.superficialtrauma.common.body;

import java.util.Locale;

public enum DownedPosture {
    STANDING,
    CROUCHING,
    SPRINTING,
    SWIMMING,
    CRAWLING,
    UNSAFE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean usesFadeOnlyTransition() {
        return this == SWIMMING || this == CRAWLING || this == UNSAFE;
    }

    public static DownedPosture fromSerializedName(String name) {
        if (name == null || name.isBlank()) {
            return UNSAFE;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNSAFE;
        }
    }
}
