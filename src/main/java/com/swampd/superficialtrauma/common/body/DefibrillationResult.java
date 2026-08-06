package com.swampd.superficialtrauma.common.body;

public record DefibrillationResult(
        Status status,
        DefibrillationEnergy energy,
        int attemptMask
) {
    public enum Status {
        INVALID,
        FAILED,
        RESTORED_CIRCULATION,
        UNSAFE_FAILURE_BRAIN_DEATH
    }

    public boolean valid() {
        return status != Status.INVALID;
    }

    public boolean succeeded() {
        return status == Status.RESTORED_CIRCULATION;
    }
}
