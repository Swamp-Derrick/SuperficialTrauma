package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.common.wound.WoundType;

import java.util.Optional;

public record DamageClassification(WoundType woundType, String reason) {
    public static DamageClassification blunt(String reason) {
        return new DamageClassification(WoundType.BLUNT, reason);
    }

    public static DamageClassification deferred(String reason) {
        return new DamageClassification(null, reason);
    }

    public Optional<WoundType> woundTypeOptional() {
        return Optional.ofNullable(woundType);
    }
}
