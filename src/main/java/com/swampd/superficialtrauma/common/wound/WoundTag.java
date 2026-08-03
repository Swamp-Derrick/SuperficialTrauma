package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundTag {
    SLOWNESS_1,
    PAIN_1,
    MOVEMENT_BLEEDING_1;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WoundTag fromSerializedName(String name) {
        for (WoundTag tag : values()) {
            if (tag.serializedName().equals(name)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Unknown wound tag: " + name);
    }
}
