package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundType {
    BLUNT,
    SHARP,
    BURN,
    EXPLOSION;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "wound_type.superficialtrauma." + serializedName();
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
