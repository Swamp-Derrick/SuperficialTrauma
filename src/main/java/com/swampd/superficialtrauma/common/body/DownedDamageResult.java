package com.swampd.superficialtrauma.common.body;

public record DownedDamageResult(
        boolean applied,
        long shortenedTicks,
        long remainingTicks,
        BodyLifeState previousState,
        BodyLifeState resultingState
) {
    public static DownedDamageResult ignored(BodyLifeState state) {
        return new DownedDamageResult(false, 0L, 0L, state, state);
    }

    public boolean stateChanged() {
        return previousState != resultingState;
    }
}
