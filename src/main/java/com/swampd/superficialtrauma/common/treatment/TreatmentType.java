package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum TreatmentType {
    TEMPORARY_DRESSING(5L * 20L, Items.LEATHER, 1);

    private final long durationTicks;
    private final Item requiredItem;
    private final int requiredCount;

    TreatmentType(long durationTicks, Item requiredItem, int requiredCount) {
        this.durationTicks = durationTicks;
        this.requiredItem = requiredItem;
        this.requiredCount = requiredCount;
    }

    public long durationTicks() {
        return durationTicks;
    }

    public Item requiredItem() {
        return requiredItem;
    }

    public int requiredCount() {
        return requiredCount;
    }

    public boolean supports(WoundInstance wound) {
        if (this != TEMPORARY_DRESSING || wound == null || wound.isHealed()) {
            return false;
        }
        return wound.temporaryDressingApplied()
                || wound.bleedingLevel(true) > 0
                || wound.type() == WoundType.EXPLOSION
                || (wound.type() == WoundType.BLUNT && wound.severity() >= 3);
    }

    public TreatmentAction actionFor(WoundInstance wound) {
        return wound != null && wound.temporaryDressingApplied()
                ? TreatmentAction.REMOVE
                : TreatmentAction.APPLY;
    }

    public boolean isApplicable(WoundInstance wound, TreatmentAction action) {
        if (!supports(wound) || action == null) {
            return false;
        }
        return action == actionFor(wound);
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "treatment.superficialtrauma." + serializedName();
    }

    public static TreatmentType fromSerializedName(String name) {
        for (TreatmentType type : values()) {
            if (type.serializedName().equals(name)) {
                return type;
            }
        }
        return TEMPORARY_DRESSING;
    }
}
