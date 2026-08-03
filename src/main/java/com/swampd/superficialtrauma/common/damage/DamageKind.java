package com.swampd.superficialtrauma.common.damage;

import java.util.Locale;

public enum DamageKind {
    UNKNOWN,
    BLUNT,
    CGM_LOW_VELOCITY,
    CGM_HIGH_VELOCITY,
    CGM_SHOTGUN,
    CGM_UNCLASSIFIED,
    DEFERRED;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "damage_kind.superficialtrauma." + serializedName();
    }

    public static DamageKind fromSerializedName(String name) {
        for (DamageKind kind : values()) {
            if (kind.serializedName().equals(name)) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
