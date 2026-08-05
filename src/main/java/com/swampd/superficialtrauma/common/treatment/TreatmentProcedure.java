package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;

import java.util.List;
import java.util.Locale;

public enum TreatmentProcedure {
    TEMPORARY_DRESSING(
            WoundCovering.TEMPORARY_DRESSING,
            TreatmentType.TEMPORARY_DRESSING,
            new TreatmentIngredient(TreatmentType.TEMPORARY_DRESSING, 1)
    ),
    SELF_ADHESIVE_BANDAGE(
            WoundCovering.SELF_ADHESIVE_BANDAGE,
            TreatmentType.SELF_ADHESIVE_BANDAGE,
            new TreatmentIngredient(TreatmentType.SELF_ADHESIVE_BANDAGE, 1)
    ),
    BANDAGE_WITH_MEDICAL_TAPE(
            WoundCovering.BANDAGE_WITH_MEDICAL_TAPE,
            TreatmentType.BANDAGE,
            new TreatmentIngredient(TreatmentType.BANDAGE, 1),
            new TreatmentIngredient(TreatmentType.MEDICAL_TAPE, 1)
    ),
    BANDAGE_WITH_SELF_ADHESIVE_BANDAGE(
            WoundCovering.BANDAGE_WITH_SELF_ADHESIVE_BANDAGE,
            TreatmentType.BANDAGE,
            new TreatmentIngredient(TreatmentType.BANDAGE, 1),
            new TreatmentIngredient(TreatmentType.SELF_ADHESIVE_BANDAGE, 1)
    );

    private static final long DURATION_TICKS = 5L * 20L;

    private final WoundCovering covering;
    private final TreatmentType removalAnchor;
    private final List<TreatmentIngredient> ingredients;

    TreatmentProcedure(
            WoundCovering covering,
            TreatmentType removalAnchor,
            TreatmentIngredient... ingredients
    ) {
        this.covering = covering;
        this.removalAnchor = removalAnchor;
        this.ingredients = List.of(ingredients);
    }

    public long durationTicks() {
        return DURATION_TICKS;
    }

    public WoundCovering covering() {
        return covering;
    }

    public TreatmentType removalAnchor() {
        return removalAnchor;
    }

    public List<TreatmentIngredient> ingredients() {
        return ingredients;
    }

    public boolean supports(WoundInstance wound) {
        if (wound == null || wound.isHealed()) {
            return false;
        }
        return wound.covering().isApplied()
                || untreatedBleedingLevel(wound) > 0
                || wound.type() == WoundType.EXPLOSION
                || (wound.type() == WoundType.BLUNT && wound.severity() >= 3);
    }

    public boolean isApplicable(WoundInstance wound, TreatmentAction action) {
        if (!supports(wound) || action == null) {
            return false;
        }
        return action == TreatmentAction.APPLY
                ? !wound.covering().isApplied()
                : wound.covering() == covering;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "treatment_procedure.superficialtrauma." + serializedName();
    }

    public static TreatmentProcedure forCovering(WoundCovering covering) {
        for (TreatmentProcedure procedure : values()) {
            if (procedure.covering == covering) {
                return procedure;
            }
        }
        return null;
    }

    public static TreatmentProcedure singleStepFor(TreatmentType type) {
        return switch (type) {
            case TEMPORARY_DRESSING -> TEMPORARY_DRESSING;
            case SELF_ADHESIVE_BANDAGE -> SELF_ADHESIVE_BANDAGE;
            default -> null;
        };
    }

    public static TreatmentProcedure bandageCombination(TreatmentType secondStep) {
        return switch (secondStep) {
            case MEDICAL_TAPE -> BANDAGE_WITH_MEDICAL_TAPE;
            case SELF_ADHESIVE_BANDAGE -> BANDAGE_WITH_SELF_ADHESIVE_BANDAGE;
            default -> null;
        };
    }

    public static TreatmentProcedure fromSerializedName(String name) {
        for (TreatmentProcedure procedure : values()) {
            if (procedure.serializedName().equals(name)) {
                return procedure;
            }
        }
        return TEMPORARY_DRESSING;
    }

    private static int untreatedBleedingLevel(WoundInstance wound) {
        int effective = wound.bleedingLevel(true);
        return effective + wound.covering().bleedingReduction();
    }
}
