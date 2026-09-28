package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundType {
    BLUNT,
    SHARP,
    PUNCTURE,
    CRUSH,
    FROSTBITE,
    BURN,
    EXPLOSION,
    GUNSHOT_LOW_VELOCITY,
    GUNSHOT_HIGH_VELOCITY,
    GUNSHOT_SHOTGUN;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "wound_type.superficialtrauma." + serializedName();
    }

    public boolean isGunshot() {
        return this == GUNSHOT_LOW_VELOCITY
                || this == GUNSHOT_HIGH_VELOCITY
                || this == GUNSHOT_SHOTGUN;
    }

    public static WoundType fromSerializedName(String name) {
        for (WoundType type : values()) {
            if (type.serializedName().equals(name)) {
                return type;
            }
        }
        return BLUNT;
    }
}
