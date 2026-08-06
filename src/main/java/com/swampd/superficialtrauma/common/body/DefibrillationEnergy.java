package com.swampd.superficialtrauma.common.body;

import java.util.Arrays;

public enum DefibrillationEnergy {
    J150(150, 0.30D, 1),
    J200(200, 0.35D, 2),
    J250(250, 0.50D, 4),
    J300(300, 0.60D, 8);

    private final int joules;
    private final double successChance;
    private final int attemptBit;

    DefibrillationEnergy(int joules, double successChance, int attemptBit) {
        this.joules = joules;
        this.successChance = successChance;
        this.attemptBit = attemptBit;
    }

    public int joules() {
        return joules;
    }

    public double successChance() {
        return successChance;
    }

    public int attemptBit() {
        return attemptBit;
    }

    public boolean isUnsafeWithoutEscalation() {
        return this == J250 || this == J300;
    }

    public static DefibrillationEnergy fromJoules(int joules) {
        return Arrays.stream(values())
                .filter(value -> value.joules == joules)
                .findFirst()
                .orElse(J150);
    }
}
