package com.swampd.superficialtrauma.common.medication;

import java.util.Locale;
import java.util.Optional;

public enum MedicationType {
    PARACETAMOL(
            MedicationRoute.ORAL,
            MedicationFamily.NON_OPIOID,
            50L,
            3L * 60L * 20L,
            4.0F,
            1.0F,
            0,
            0
    ),
    MORPHINE(
            MedicationRoute.INJECTION,
            MedicationFamily.OPIOID,
            5L * 20L,
            3L * 60L * 20L,
            6.0F,
            2.0F,
            0,
            1
    ),
    REMIFENTANIL(
            MedicationRoute.INJECTION,
            MedicationFamily.OPIOID,
            5L * 20L,
            60L * 20L,
            14.0F,
            16.0F,
            0,
            3
    ),
    NALOXONE(
            MedicationRoute.INJECTION,
            MedicationFamily.OPIOID_ANTAGONIST,
            5L * 20L,
            0L,
            0.0F,
            0.0F,
            0,
            0
    ),
    EPINEPHRINE(
            MedicationRoute.INJECTION,
            MedicationFamily.ADRENERGIC,
            5L * 20L,
            3L * 60L * 20L,
            5.0F,
            0.0F,
            1,
            0
    ),
    METOPROLOL(
            MedicationRoute.ORAL,
            MedicationFamily.BETA_BLOCKER,
            50L,
            5L * 60L * 20L,
            4.0F,
            0.0F,
            -2,
            0
    ),
    ATROPINE_SULFATE(
            MedicationRoute.INJECTION,
            MedicationFamily.ANTICHOLINERGIC,
            5L * 20L,
            3L * 60L * 20L,
            3.0F,
            0.0F,
            1,
            0
    ),
    PRALIDOXIME_CHLORIDE(
            MedicationRoute.INJECTION,
            MedicationFamily.OXIME,
            5L * 20L,
            60L * 20L,
            2.0F,
            0.0F,
            0,
            0
    ),
    AMOXICILLIN(
            MedicationRoute.ORAL,
            MedicationFamily.ANTIBIOTIC,
            50L,
            3L * 60L * 20L,
            5.0F,
            0.0F,
            0,
            0
    ),
    CEFTRIAXONE(
            MedicationRoute.INJECTION,
            MedicationFamily.ANTIBIOTIC,
            5L * 20L,
            3L * 60L * 20L,
            5.0F,
            0.0F,
            0,
            0
    );

    private final MedicationRoute route;
    private final MedicationFamily family;
    private final long actionDurationTicks;
    private final long effectDurationTicks;
    private final float concentration;
    private final float painReduction;
    private final int heartRateShift;
    private final int opioidEquivalentLayers;

    MedicationType(
            MedicationRoute route,
            MedicationFamily family,
            long actionDurationTicks,
            long effectDurationTicks,
            float concentration,
            float painReduction,
            int heartRateShift,
            int opioidEquivalentLayers
    ) {
        this.route = route;
        this.family = family;
        this.actionDurationTicks = actionDurationTicks;
        this.effectDurationTicks = effectDurationTicks;
        this.concentration = concentration;
        this.painReduction = painReduction;
        this.heartRateShift = heartRateShift;
        this.opioidEquivalentLayers = Math.max(0, opioidEquivalentLayers);
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

    public int opioidEquivalentLayers() {
        return opioidEquivalentLayers;
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

    public static MedicationType fromStoredName(String name) {
        return fromNetworkName(name).orElse(PARACETAMOL);
    }

    public static Optional<MedicationType> fromNetworkName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (MedicationType type : values()) {
            if (type.serializedName().equals(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
