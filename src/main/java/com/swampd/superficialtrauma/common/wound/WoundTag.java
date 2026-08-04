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
    BLEEDING_4,
    DISORIENTATION_1,
    DISORIENTATION_2,
    DISORIENTATION_3,
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

    public int bleedingLevel() {
        return switch (this) {
            case BLEEDING_1, MOVEMENT_BLEEDING_1 -> 1;
            case BLEEDING_2 -> 2;
            case BLEEDING_3 -> 3;
            case BLEEDING_4 -> 4;
            default -> 0;
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
