package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundDisinfectant;
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
    ),
    WOUND_PACKING(
            null,
            TreatmentType.MEDICAL_GAUZE,
            new TreatmentIngredient(TreatmentType.MEDICAL_GAUZE, 1)
    ),
    ICE_PACK(
            null,
            TreatmentType.ICE_PACK,
            new TreatmentIngredient(TreatmentType.ICE_PACK, 1)
    ),
    TOURNIQUET(
            null,
            TreatmentType.TOURNIQUET,
            8L * 20L,
            new TreatmentIngredient(TreatmentType.TOURNIQUET, 1)
    ),
    POVIDONE_IODINE(
            null,
            TreatmentType.POVIDONE_IODINE,
            new TreatmentIngredient(TreatmentType.POVIDONE_IODINE, 1)
    ),
    MEDICAL_ALCOHOL(
            null,
            TreatmentType.MEDICAL_ALCOHOL,
            new TreatmentIngredient(TreatmentType.MEDICAL_ALCOHOL, 1)
    ),
    DEBRIDEMENT(
            null,
            TreatmentType.SURGICAL_KIT,
            12L * 20L,
            new TreatmentIngredient(TreatmentType.SALINE_SOLUTION, 1),
            new TreatmentIngredient(TreatmentType.SURGICAL_KIT, 1)
    ),
    CHEST_SEAL(null, TreatmentType.CHEST_SEAL, 3L * 20L,
            new TreatmentIngredient(TreatmentType.CHEST_SEAL, 1)),
    PNEUMOTHORAX_REPAIR(null, TreatmentType.MEDICAL_GAUZE, 45L * 20L,
            new TreatmentIngredient(TreatmentType.SURGICAL_KIT, 1),
            new TreatmentIngredient(TreatmentType.MEDICAL_GAUZE, 1)),
    SKIN_GRAFT(
            null,
            TreatmentType.ARTIFICIAL_DERMIS,
            50L * 20L,
            new TreatmentIngredient(TreatmentType.SURGICAL_KIT, 1),
            new TreatmentIngredient(TreatmentType.ARTIFICIAL_DERMIS, 1)
    );

    private static final long DEFAULT_DURATION_TICKS = 5L * 20L;

    private final WoundCovering covering;
    private final TreatmentType removalAnchor;
    private final List<TreatmentIngredient> ingredients;
    private final long durationTicks;

    TreatmentProcedure(
            WoundCovering covering,
            TreatmentType removalAnchor,
            TreatmentIngredient... ingredients
    ) {
        this(covering, removalAnchor, DEFAULT_DURATION_TICKS, ingredients);
    }

    TreatmentProcedure(
            WoundCovering covering,
            TreatmentType removalAnchor,
            long durationTicks,
            TreatmentIngredient... ingredients
    ) {
        this.covering = covering;
        this.removalAnchor = removalAnchor;
        this.ingredients = List.of(ingredients);
        this.durationTicks = Math.max(1L, durationTicks);
    }

    public long durationTicks() {
        return durationTicks;
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
        if (this == CHEST_SEAL || this == PNEUMOTHORAX_REPAIR) return wound.pneumothoraxWound();
        if (isDebridement()) {
            return wound.woundTags().contains(com.swampd.superficialtrauma.common.wound.WoundTag.NEEDS_DEBRIDEMENT_1)
                    || wound.isInfected();
        }
        if (isSkinGraft()) {
            return wound.hasNecrosis();
        }
        if (isDisinfection()) {
            return wound.canDisinfect();
        }
        if (isIcePack()) {
            return wound.canApplyIcePack();
        }
        if (isTourniquet()) {
            return wound.tourniquetApplied() || wound.canApplyTourniquet();
        }
        if (isWoundPacking()) {
            return wound.pneumothoraxWound() || wound.untreatedBleedingLevel(true) > 0;
        }
        return wound.pneumothoraxWound() || wound.covering().isApplied()
                || untreatedBleedingLevel(wound) > 0
                || wound.type() == WoundType.EXPLOSION
                || (wound.type() == WoundType.BLUNT && wound.severity() >= 3);
    }

    public boolean isApplicable(WoundInstance wound, TreatmentAction action) {
        if (!supports(wound) || action == null) {
            return false;
        }
        if (this == CHEST_SEAL) return action == TreatmentAction.APPLY && !wound.chestSealApplied();
        if (this == PNEUMOTHORAX_REPAIR) return action == TreatmentAction.APPLY;
        if (isDebridement()) {
            return action == TreatmentAction.APPLY && wound.canDebride();
        }
        if (isSkinGraft()) {
            return action == TreatmentAction.APPLY && wound.canSkinGraft();
        }
        if (isDisinfection()) {
            return action == TreatmentAction.APPLY && wound.canDisinfect();
        }
        if (isIcePack()) {
            return action == TreatmentAction.APPLY && wound.canApplyIcePack();
        }
        if (isTourniquet()) {
            return action == TreatmentAction.APPLY
                    ? wound.canApplyTourniquet()
                    : wound.tourniquetApplied();
        }
        if (isWoundPacking()) {
            return action == TreatmentAction.APPLY
                    ? !wound.woundPackingApplied()
                    : wound.woundPackingApplied();
        }
        return action == TreatmentAction.APPLY
                ? !wound.covering().isApplied()
                : wound.covering() == covering;
    }

    public boolean isWoundPacking() {
        return this == WOUND_PACKING;
    }

    public boolean isDebridement() {
        return this == DEBRIDEMENT;
    }

    public boolean isSkinGraft() {
        return this == SKIN_GRAFT;
    }

    public boolean usesQte() { return isSkinGraft() || this == PNEUMOTHORAX_REPAIR; }

    public boolean isIcePack() {
        return this == ICE_PACK;
    }

    public boolean isTourniquet() {
        return this == TOURNIQUET;
    }

    public boolean isDisinfection() {
        return this == POVIDONE_IODINE || this == MEDICAL_ALCOHOL;
    }

    public WoundDisinfectant disinfectant() {
        return switch (this) {
            case POVIDONE_IODINE -> WoundDisinfectant.POVIDONE_IODINE;
            case MEDICAL_ALCOHOL -> WoundDisinfectant.MEDICAL_ALCOHOL;
            default -> null;
        };
    }

    public boolean requiresSurgerySkill() {
        return isDebridement() || usesQte();
    }

    public boolean requiresFirstAidSkill() {
        return isTourniquet();
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "treatment_procedure.superficialtrauma." + serializedName();
    }

    public static TreatmentProcedure forCovering(WoundCovering covering) {
        for (TreatmentProcedure procedure : values()) {
            if (procedure.covering != null && procedure.covering == covering) {
                return procedure;
            }
        }
        return null;
    }

    public static TreatmentProcedure singleStepFor(TreatmentType type) {
        return switch (type) {
            case TEMPORARY_DRESSING -> TEMPORARY_DRESSING;
            case SELF_ADHESIVE_BANDAGE -> SELF_ADHESIVE_BANDAGE;
            case MEDICAL_GAUZE -> WOUND_PACKING;
            case ICE_PACK -> ICE_PACK;
            case TOURNIQUET -> TOURNIQUET;
            case POVIDONE_IODINE -> POVIDONE_IODINE;
            case MEDICAL_ALCOHOL -> MEDICAL_ALCOHOL;
            case SURGICAL_KIT -> DEBRIDEMENT;
            case CHEST_SEAL -> CHEST_SEAL;
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

    public static boolean supportsType(WoundInstance wound, TreatmentType type) {
        if (wound == null || type == null) {
            return false;
        }
        return switch (type) {
            case TEMPORARY_DRESSING -> TEMPORARY_DRESSING.supports(wound);
            case BANDAGE -> BANDAGE_WITH_MEDICAL_TAPE.supports(wound)
                    || BANDAGE_WITH_SELF_ADHESIVE_BANDAGE.supports(wound);
            case MEDICAL_TAPE -> BANDAGE_WITH_MEDICAL_TAPE.supports(wound);
            case SELF_ADHESIVE_BANDAGE -> SELF_ADHESIVE_BANDAGE.supports(wound)
                    || BANDAGE_WITH_SELF_ADHESIVE_BANDAGE.supports(wound);
            case MEDICAL_GAUZE -> WOUND_PACKING.supports(wound) || PNEUMOTHORAX_REPAIR.supports(wound);
            case ICE_PACK -> ICE_PACK.supports(wound);
            case TOURNIQUET -> TOURNIQUET.supports(wound);
            case POVIDONE_IODINE -> POVIDONE_IODINE.supports(wound);
            case MEDICAL_ALCOHOL -> MEDICAL_ALCOHOL.supports(wound);
            case SALINE_SOLUTION -> DEBRIDEMENT.supports(wound);
            case SURGICAL_KIT -> DEBRIDEMENT.supports(wound) || SKIN_GRAFT.supports(wound) || PNEUMOTHORAX_REPAIR.supports(wound);
            case ARTIFICIAL_DERMIS -> SKIN_GRAFT.supports(wound);
            case CHEST_SEAL -> CHEST_SEAL.supports(wound);
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
        return wound.untreatedBleedingLevel(true);
    }
}
