package com.swampd.superficialtrauma.common.body;

import java.util.Locale;

public enum DownedFallDirection {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    FADE_ONLY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DownedFallDirection fromSerializedName(String name) {
        if (name == null || name.isBlank()) {
            return FADE_ONLY;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FADE_ONLY;
        }
    }
}
