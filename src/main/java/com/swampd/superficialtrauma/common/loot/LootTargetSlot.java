package com.swampd.superficialtrauma.common.loot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class LootTargetSlot extends Slot {
    LootTargetSlot(Container container, int containerSlot, int x, int y) {
        super(container, containerSlot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
