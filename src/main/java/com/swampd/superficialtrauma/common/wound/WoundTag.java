package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundTag {
    SLOWNESS_1,
    PAIN_1,
    PAIN_2,
    PAIN_3,
    MOVEMENT_BLEEDING_1,
    BLEEDING_1,
    BLEEDING_2,
    BLEEDING_3,
    DISORIENTATION_1,
    NECROSIS_3,
    NEEDS_DEBRIDEMENT_1;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public float painContribution() {
        return switch (this) {
            case PAIN_1 -> 1.0F;
            case PAIN_2 -> 2.0F;
            case PAIN_3 -> 4.0F;
            default -> 0.0F;
        };
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
