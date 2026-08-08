package com.swampd.superficialtrauma.common.body;

public enum HealthStatus {
    OK,
    VERY_MINOR_DAMAGE,
    MINOR_DAMAGE,
    MODERATE_DAMAGE,
    SEVERE_DAMAGE,
    TERMINAL_DAMAGE,
    DOWNED;

    public static HealthStatus from(float health, boolean downed) {
        if (downed) {
            return DOWNED;
        }
        float finiteHealth = Float.isFinite(health) ? health : 0.0F;
        if (finiteHealth >= 18.0F) {
            return OK;
        }
        if (finiteHealth >= 16.0F) {
            return VERY_MINOR_DAMAGE;
        }
        if (finiteHealth >= 12.0F) {
            return MINOR_DAMAGE;
        }
        if (finiteHealth >= 8.0F) {
            return MODERATE_DAMAGE;
        }
        if (finiteHealth >= 4.0F) {
            return SEVERE_DAMAGE;
        }
        return TERMINAL_DAMAGE;
    }

    public String translationKey() {
        return "health_status.superficialtrauma." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
