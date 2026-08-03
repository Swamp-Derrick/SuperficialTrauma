package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class DamageItemTags {
    public static final TagKey<Item> SHARP_WEAPONS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "weapons/sharp")
    );

    private DamageItemTags() {
    }
}
