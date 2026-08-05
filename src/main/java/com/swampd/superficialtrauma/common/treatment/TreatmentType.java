package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

public enum TreatmentType {
    TEMPORARY_DRESSING(() -> Items.LEATHER),
    BANDAGE(ModItems.BANDAGE::get),
    MEDICAL_TAPE(ModItems.MEDICAL_TAPE::get),
    SELF_ADHESIVE_BANDAGE(ModItems.SELF_ADHESIVE_BANDAGE::get),
    MEDICAL_GAUZE(ModItems.MEDICAL_GAUZE::get),
    ICE_PACK(ModItems.ICE_PACK::get),
    TOURNIQUET(ModItems.TOURNIQUET::get),
    SALINE_SOLUTION(ModItems.SALINE_SOLUTION::get),
    SURGICAL_KIT(ModItems.SURGICAL_KIT::get);

    private final Supplier<Item> requiredItem;

    TreatmentType(Supplier<Item> requiredItem) {
        this.requiredItem = requiredItem;
    }

    public Item requiredItem() {
        return requiredItem.get();
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
