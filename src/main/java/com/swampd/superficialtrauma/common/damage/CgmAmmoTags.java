package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CgmAmmoTags {
    public static final TagKey<Item> LOW_VELOCITY = create("ammo/low_velocity");
    public static final TagKey<Item> HIGH_VELOCITY = create("ammo/high_velocity");
    public static final TagKey<Item> SHOTGUN = create("ammo/shotgun");

    private CgmAmmoTags() {
    }

    public static DamageKind classify(ItemStack ammoStack) {
        if (ammoStack.isEmpty()) {
            return DamageKind.CGM_UNCLASSIFIED;
        }
        if (ammoStack.is(SHOTGUN)) {
            return DamageKind.CGM_SHOTGUN;
        }
        if (ammoStack.is(HIGH_VELOCITY)) {
            return DamageKind.CGM_HIGH_VELOCITY;
        }
        if (ammoStack.is(LOW_VELOCITY)) {
            return DamageKind.CGM_LOW_VELOCITY;
        }
        return DamageKind.CGM_UNCLASSIFIED;
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, path)
        );
    }
}
