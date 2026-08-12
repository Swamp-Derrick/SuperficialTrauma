package com.swampd.superficialtrauma.common.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record MedicalWorkbenchRecipe(
        String key,
        int id,
        Supplier<? extends Item> resultItem,
        int resultCount,
        int craftTimeTicks,
        List<MedicalWorkbenchIngredient> ingredients
) {
    public MedicalWorkbenchRecipe {
        if (key == null || key.isBlank() || id < 0 || resultCount <= 0
                || craftTimeTicks <= 0 || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Invalid medical workbench recipe");
        }
        ingredients = List.copyOf(ingredients);
    }

    public ItemStack resultStack() {
        return new ItemStack(resultItem.get(), resultCount);
    }
}
