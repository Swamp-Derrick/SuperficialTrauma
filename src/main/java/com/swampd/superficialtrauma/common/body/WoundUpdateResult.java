package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.wound.WoundInstance;

import java.util.Optional;

public record WoundUpdateResult(Status status, WoundInstance wound, float accumulatedDamage) {
    public enum Status {
        PENDING,
        CREATED,
        UPDATED,
        LIMIT_REACHED
    }

    public Optional<WoundInstance> woundOptional() {
        return Optional.ofNullable(wound);
    }

    public boolean changedBodyState() {
        return status != Status.LIMIT_REACHED;
    }
}
