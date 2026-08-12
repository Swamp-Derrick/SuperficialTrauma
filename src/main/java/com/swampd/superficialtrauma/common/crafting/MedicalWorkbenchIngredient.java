package com.swampd.superficialtrauma.common.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record MedicalWorkbenchIngredient(Item item, int count) {
    public MedicalWorkbenchIngredient {
        if (count <= 0) {
            throw new IllegalArgumentException("Ingredient count must be positive");
        }
    }

    public ItemStack displayStack() {
        return new ItemStack(item, count);
    }
}
