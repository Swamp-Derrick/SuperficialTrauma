package com.swampd.superficialtrauma.common.body;

public enum PainSensation {
    NONE,
    MINOR_PAIN,
    PAIN,
    SEVERE_PAIN,
    EXTREME_PAIN;

    public static PainSensation from(float pain) {
        float finitePain = Float.isFinite(pain) ? pain : 0.0F;
        if (finitePain <= 0.0F) {
            return NONE;
        }
        if (finitePain < 5.0F) {
            return MINOR_PAIN;
        }
        if (finitePain < 12.0F) {
            return PAIN;
        }
        if (finitePain < 18.0F) {
            return SEVERE_PAIN;
        }
        return EXTREME_PAIN;
    }

    public String translationKey() {
        return "pain_sensation.superficialtrauma." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
