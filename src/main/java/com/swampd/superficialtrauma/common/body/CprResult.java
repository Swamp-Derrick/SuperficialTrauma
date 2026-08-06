package com.swampd.superficialtrauma.common.body;

public record CprResult(
        Status status,
        int accumulatedSeconds,
        double successChance
) {
    public enum Status {
        INVALID,
        CONTINUE,
        RESTORED_CIRCULATION,
        VENTRICULAR_FIBRILLATION
    }

    public boolean succeeded() {
        return status == Status.RESTORED_CIRCULATION
                || status == Status.VENTRICULAR_FIBRILLATION;
    }

    public static CprResult invalid(int seconds, double chance) {
        return new CprResult(Status.INVALID, seconds, chance);
    }
}
