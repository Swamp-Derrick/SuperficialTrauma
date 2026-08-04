package com.swampd.superficialtrauma.common.damage;

public final class DamageDowning {
    public static final float PRESERVED_HEALTH = 1.0F;

    private DamageDowning() {
    }

    public static boolean wouldBeFatal(float currentHealth, float finalDamage) {
        return currentHealth > 0.0F && finalDamage >= currentHealth;
    }

    public static float clampToPreserveLife(float currentHealth, float finalDamage) {
        if (currentHealth <= 0.0F || finalDamage <= 0.0F) {
            return 0.0F;
        }

        float healthFloor = Math.min(currentHealth, PRESERVED_HEALTH);
        return Math.min(finalDamage, Math.max(0.0F, currentHealth - healthFloor));
    }
}
