package com.swampd.superficialtrauma.common.body;

import java.util.Locale;

public enum CollapseReason {
    NONE,
    LETHAL_DAMAGE,
    HEMORRHAGIC_SHOCK,
    TRAUMATIC_SHOCK,
    SEPSIS,
    OVERDOSE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "collapse_reason.superficialtrauma." + serializedName();
    }

    public static CollapseReason fromSerializedName(String name) {
        for (CollapseReason reason : values()) {
            if (reason.serializedName().equals(name)) {
                return reason;
            }
        }
        return NONE;
    }
}
