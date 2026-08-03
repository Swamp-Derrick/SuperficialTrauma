package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundType {
    BLUNT;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
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
