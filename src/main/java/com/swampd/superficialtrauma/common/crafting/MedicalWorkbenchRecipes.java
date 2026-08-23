package com.swampd.superficialtrauma.common.crafting;

import com.swampd.superficialtrauma.common.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class MedicalWorkbenchRecipes {
    private static final List<MedicalWorkbenchRecipe> RECIPES = buildRecipes();

    private MedicalWorkbenchRecipes() {
    }

    public static List<MedicalWorkbenchRecipe> all() {
        return RECIPES;
    }

    public static MedicalWorkbenchRecipe byId(int id) {
        return id >= 0 && id < RECIPES.size() ? RECIPES.get(id) : null;
    }

    public static MedicalWorkbenchRecipe byKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return RECIPES.stream().filter(recipe -> recipe.key().equals(key)).findFirst().orElse(null);
    }

    private static List<MedicalWorkbenchRecipe> buildRecipes() {
        List<MedicalWorkbenchRecipe> recipes = new ArrayList<>();
        add(recipes, "bandage", ModItems.BANDAGE, 1, 15,
                ingredient(Items.STRING, 3), ingredient(Items.WHITE_WOOL, 1));
        add(recipes, "medical_tape", ModItems.MEDICAL_TAPE, 1, 20,
                ingredient(Items.PAPER, 2), ingredient(Items.SLIME_BALL, 1));
        add(recipes, "self_adhesive_bandage", ModItems.SELF_ADHESIVE_BANDAGE, 1, 25,
                ingredient(Items.STRING, 2), ingredient(Items.WHITE_WOOL, 1),
                ingredient(Items.SLIME_BALL, 1));
        add(recipes, "medical_gauze", ModItems.MEDICAL_GAUZE, 2, 20,
                ingredient(Items.STRING, 4), ingredient(Items.WHITE_WOOL, 1));
        add(recipes, "ice_pack", ModItems.ICE_PACK, 1, 30,
                ingredient(Items.LEATHER, 1), ingredient(Items.ICE, 2));
        add(recipes, "tourniquet", ModItems.TOURNIQUET, 1, 45,
                ingredient(Items.LEATHER, 2), ingredient(Items.STRING, 2),
                ingredient(Items.IRON_NUGGET, 1));
        add(recipes, "saline_solution", ModItems.SALINE_SOLUTION, 1, 45,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.SNOWBALL, 2));
        add(recipes, "blood_bag", ModItems.BLOOD_BAG, 1, 60,
                ingredient(Items.LEATHER, 2), ingredient(Items.GLASS_BOTTLE, 1),
                ingredient(Items.RED_DYE, 2));
        add(recipes, "syringe", ModItems.SYRINGE, 2, 30,
                ingredient(Items.IRON_NUGGET, 2), ingredient(Items.GLASS_PANE, 1));
        add(recipes, "paracetamol", ModItems.PARACETAMOL, 2, 30,
                ingredient(Items.SUGAR, 2), ingredient(Items.PAPER, 1));
        add(recipes, "morphine_vial", ModItems.MORPHINE_VIAL, 1, 60,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.POPPY, 2),
                ingredient(Items.SUGAR, 1));
        add(recipes, "naloxone", ModItems.NALOXONE, 1, 75,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.SPIDER_EYE, 1),
                ingredient(Items.SUGAR, 2));
        add(recipes, "epinephrine_injection", ModItems.EPINEPHRINE_INJECTION, 1, 75,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.REDSTONE, 2),
                ingredient(Items.SUGAR, 1));
        add(recipes, "metoprolol", ModItems.METOPROLOL, 2, 45,
                ingredient(Items.SUGAR, 2), ingredient(Items.BROWN_MUSHROOM, 1),
                ingredient(Items.PAPER, 1));
        add(recipes, "stethoscope", ModItems.STETHOSCOPE, 1, 90,
                ingredient(Items.LEATHER, 2), ingredient(Items.IRON_NUGGET, 3));
        add(recipes, "manual_resuscitator", ModItems.MANUAL_RESUSCITATOR, 1, 120,
                ingredient(Items.LEATHER, 3), ingredient(Items.GLASS_BOTTLE, 1),
                ingredient(Items.IRON_INGOT, 1));
        add(recipes, "surgical_kit", ModItems.SURGICAL_KIT, 1, 150,
                ingredient(Items.IRON_INGOT, 2), ingredient(Items.SHEARS, 1),
                ingredient(Items.LEATHER, 2), ingredient(Items.STRING, 2));
        add(recipes, "pupil_penlight", ModItems.PUPIL_PENLIGHT, 1, 45,
                ingredient(Items.TORCH, 1), ingredient(Items.IRON_NUGGET, 2),
                ingredient(Items.GLASS_PANE, 1));
        add(recipes, "checklist", ModItems.CHECKLIST, 1, 20,
                ingredient(Items.PAPER, 3), ingredient(Items.INK_SAC, 1));
        add(recipes, "defibrillator", ModItems.DEFIBRILLATOR, 1, 180,
                ingredient(Items.IRON_INGOT, 4), ingredient(Items.COPPER_INGOT, 2),
                ingredient(Items.REDSTONE, 3), ingredient(Items.LEATHER, 1));
        add(recipes, "ddvp_insecticide", ModItems.DDVP_INSECTICIDE, 1, 75,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.FERMENTED_SPIDER_EYE, 1),
                ingredient(Items.GUNPOWDER, 1), ingredient(Items.POISONOUS_POTATO, 1));
        add(recipes, "atropine_sulfate_injection", ModItems.ATROPINE_SULFATE_INJECTION, 1, 90,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.SPIDER_EYE, 1),
                ingredient(Items.SUGAR, 2), ingredient(Items.REDSTONE, 1));
        add(recipes, "pralidoxime_chloride_injection", ModItems.PRALIDOXIME_CHLORIDE_INJECTION, 1, 120,
                ingredient(Items.GLASS_BOTTLE, 1), ingredient(Items.QUARTZ, 1),
                ingredient(Items.BLAZE_POWDER, 1), ingredient(Items.SUGAR, 1));
        return Collections.unmodifiableList(recipes);
    }

    private static void add(
            List<MedicalWorkbenchRecipe> recipes,
            String key,
            Supplier<? extends Item> result,
            int resultCount,
            int seconds,
            MedicalWorkbenchIngredient... ingredients
    ) {
        recipes.add(new MedicalWorkbenchRecipe(
                key,
                recipes.size(),
                result,
                resultCount,
                seconds * 20,
                List.of(ingredients)
        ));
    }

    private static MedicalWorkbenchIngredient ingredient(Item item, int count) {
        return new MedicalWorkbenchIngredient(item, count);
    }
}
