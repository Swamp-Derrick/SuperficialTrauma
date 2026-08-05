package com.swampd.superficialtrauma.common.body;

import java.util.Locale;

public enum InfusionType {
    NONE(0.0F),
    BLOOD_BAG(0.5F),
    SALINE(0.25F);

    private final float healingPerPulse;

    InfusionType(float healingPerPulse) {
        this.healingPerPulse = healingPerPulse;
    }

    public float healingPerPulse() {
        return healingPerPulse;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "infusion.superficialtrauma." + serializedName();
    }

    public static InfusionType fromSerializedName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        for (InfusionType type : values()) {
            if (type.serializedName().equals(name)) {
                return type;
            }
        }
        return NONE;
    }
}
