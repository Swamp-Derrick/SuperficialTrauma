package com.swampd.superficialtrauma.common.wound;

import java.util.Locale;

public enum WoundCovering {
    NONE(0, 0.0F),
    TEMPORARY_DRESSING(1, 1.0F),
    SELF_ADHESIVE_BANDAGE(1, 1.0F),
    BANDAGE_WITH_MEDICAL_TAPE(2, 1.0F),
    BANDAGE_WITH_SELF_ADHESIVE_BANDAGE(3, 1.0F);

    private final int bleedingReduction;
    private final float healingPerSecond;

    WoundCovering(int bleedingReduction, float healingPerSecond) {
        this.bleedingReduction = bleedingReduction;
        this.healingPerSecond = healingPerSecond;
    }

    public int bleedingReduction() {
        return bleedingReduction;
    }

    public float healingPerSecond() {
        return healingPerSecond;
    }

    public boolean isApplied() {
        return this != NONE;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "wound_covering.superficialtrauma." + serializedName();
    }

    public static WoundCovering fromSerializedName(String name) {
        for (WoundCovering covering : values()) {
            if (covering.serializedName().equals(name)) {
                return covering;
            }
        }
        return NONE;
    }
}
