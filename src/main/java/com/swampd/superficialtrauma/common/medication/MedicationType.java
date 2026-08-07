package com.swampd.superficialtrauma.common.medication;

import java.util.Locale;

public enum MedicationType {
    PARACETAMOL(MedicationRoute.ORAL, 50L, 3L * 60L * 20L, 4.0F, 1.0F),
    MORPHINE(MedicationRoute.INJECTION, 5L * 20L, 3L * 60L * 20L, 6.0F, 2.0F);

    private final MedicationRoute route;
    private final long actionDurationTicks;
    private final long effectDurationTicks;
    private final float concentration;
    private final float painReduction;

    MedicationType(
            MedicationRoute route,
            long actionDurationTicks,
            long effectDurationTicks,
            float concentration,
            float painReduction
    ) {
        this.route = route;
        this.actionDurationTicks = actionDurationTicks;
        this.effectDurationTicks = effectDurationTicks;
        this.concentration = concentration;
        this.painReduction = painReduction;
    }

    public MedicationRoute route() {
        return route;
    }

    public long actionDurationTicks() {
        return actionDurationTicks;
    }

    public long effectDurationTicks() {
        return effectDurationTicks;
    }

    public float concentration() {
        return concentration;
    }

    public float painReduction() {
        return painReduction;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "medication.superficialtrauma." + serializedName();
    }

    public static MedicationType fromSerializedName(String name) {
        for (MedicationType type : values()) {
            if (type.serializedName().equals(name)) {
                return type;
            }
        }
        return PARACETAMOL;
    }
}
