package com.swampd.superficialtrauma.common.toxicology;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class FoodContamination {
    private static final String TAG_CONTAMINATION = "SuperficialTraumaContamination";
    private static final String TAG_SUBSTANCE = "Substance";
    private static final String DDVP = "ddvp";

    private FoodContamination() {
    }

    public static boolean isDdvpContaminated(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root == null || !root.contains(TAG_CONTAMINATION, Tag.TAG_COMPOUND)) {
            return false;
        }
        return DDVP.equals(root.getCompound(TAG_CONTAMINATION).getString(TAG_SUBSTANCE));
    }

    public static boolean contaminateWithDdvp(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isDdvpContaminated(stack)) {
            return false;
        }
        CompoundTag contamination = new CompoundTag();
        contamination.putString(TAG_SUBSTANCE, DDVP);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                data -> data.put(TAG_CONTAMINATION, contamination));
        return true;
    }
}
