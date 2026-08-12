package com.swampd.superficialtrauma.common.medication;

import java.util.Locale;

public enum MedicationType {
    PARACETAMOL(
            MedicationRoute.ORAL,
            MedicationFamily.NON_OPIOID,
            50L,
            3L * 60L * 20L,
            4.0F,
            1.0F,
            0
    ),
    MORPHINE(
            MedicationRoute.INJECTION,
            MedicationFamily.OPIOID,
            5L * 20L,
            3L * 60L * 20L,
            6.0F,
            2.0F,
            0
    ),
    NALOXONE(
            MedicationRoute.INJECTION,
            MedicationFamily.OPIOID_ANTAGONIST,
            5L * 20L,
            0L,
            0.0F,
            0.0F,
            0
    ),
    EPINEPHRINE(
            MedicationRoute.INJECTION,
            MedicationFamily.ADRENERGIC,
            5L * 20L,
            3L * 60L * 20L,
            5.0F,
            0.0F,
            1
    ),
    METOPROLOL(
            MedicationRoute.ORAL,
            MedicationFamily.BETA_BLOCKER,
            50L,
            5L * 60L * 20L,
            4.0F,
            0.0F,
            -2
    );

    private final MedicationRoute route;
    private final MedicationFamily family;
    private final long actionDurationTicks;
    private final long effectDurationTicks;
    private final float concentration;
    private final float painReduction;
    private final int heartRateShift;

    MedicationType(
            MedicationRoute route,
            MedicationFamily family,
            long actionDurationTicks,
            long effectDurationTicks,
            float concentration,
            float painReduction,
            int heartRateShift
    ) {
        this.route = route;
        this.family = family;
        this.actionDurationTicks = actionDurationTicks;
        this.effectDurationTicks = effectDurationTicks;
        this.concentration = concentration;
        this.painReduction = painReduction;
        this.heartRateShift = heartRateShift;
    }

    public MedicationRoute route() {
        return route;
    }

    public MedicationFamily family() {
        return family;
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

    public int heartRateShift() {
        return heartRateShift;
    }

    public boolean createsActiveDose() {
        return effectDurationTicks > 0L;
    }

    public boolean isOpioid() {
        return family == MedicationFamily.OPIOID;
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
