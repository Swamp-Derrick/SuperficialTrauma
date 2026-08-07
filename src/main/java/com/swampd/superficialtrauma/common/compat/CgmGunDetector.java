package com.swampd.superficialtrauma.common.compat;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Detects CGM guns without a hard compile-time dependency. CGM add-ons normally subclass the same GunItem class.
 */
public final class CgmGunDetector {
    private static final String CGM_GUN_ITEM_CLASS = "com.mrcrayfish.guns.item.GunItem";

    private CgmGunDetector() {
    }

    public static boolean isGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
            if (CGM_GUN_ITEM_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
