package com.swampd.superficialtrauma.common.body;

import java.util.Locale;

public enum BodyLifeState {
    ACTIVE,
    INCAPACITATED,
    AWAKENING,
    CARDIAC_ARREST,
    VENTRICULAR_FIBRILLATION,
    BRAIN_DEAD;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BodyLifeState fromSerializedName(String name) {
        for (BodyLifeState state : values()) {
            if (state.serializedName().equals(name)) {
                return state;
            }
        }
        return ACTIVE;
    }
}
