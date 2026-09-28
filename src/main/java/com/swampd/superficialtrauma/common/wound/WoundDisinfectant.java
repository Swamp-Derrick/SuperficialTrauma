package com.swampd.superficialtrauma.common.wound;

public enum WoundDisinfectant {
    POVIDONE_IODINE(5L * 60L * 20L, 0.5F, false),
    MEDICAL_ALCOHOL(2L * 60L * 20L, 0.0F, true);

    private final long protectionDurationTicks;
    private final float debridementWoundInfectionFloor;
    private final boolean causesAlcoholIrritation;

    WoundDisinfectant(
            long protectionDurationTicks,
            float debridementWoundInfectionFloor,
            boolean causesAlcoholIrritation
    ) {
        this.protectionDurationTicks = protectionDurationTicks;
        this.debridementWoundInfectionFloor = debridementWoundInfectionFloor;
        this.causesAlcoholIrritation = causesAlcoholIrritation;
    }

    public long protectionDurationTicks() {
        return protectionDurationTicks;
    }

    public float debridementWoundInfectionFloor() {
        return debridementWoundInfectionFloor;
    }

    public boolean causesAlcoholIrritation() {
        return causesAlcoholIrritation;
    }
}
