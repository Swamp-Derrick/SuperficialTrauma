package com.swampd.superficialtrauma.common.damage;

import java.util.Locale;

/** Forensic location; UNKNOWN means no reliable upright impact was captured. */
public enum BulletHitLocation {
    UNKNOWN, LIMBS, CHEST, HEAD;

    public String serializedName() { return name().toLowerCase(Locale.ROOT); }

    public GunshotRegion traumaRegion() {
        return switch (this) {
            case HEAD -> GunshotRegion.HEAD;
            case CHEST -> GunshotRegion.CHEST;
            default -> null;
        };
    }

    public BulletHitLocation prefer(BulletHitLocation other) {
        return other != null && other.ordinal() > ordinal() ? other : this;
    }

    public static BulletHitLocation fromSavedName(String name) {
        for (var value : values()) if (value.serializedName().equals(name)) return value;
        return UNKNOWN;
    }
}
