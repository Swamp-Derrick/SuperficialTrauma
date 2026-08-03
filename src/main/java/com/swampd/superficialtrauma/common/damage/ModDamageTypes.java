package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BLEEDING = key("bleeding");
    public static final ResourceKey<DamageType> INTERNAL_BLEEDING = key("internal_bleeding");

    private ModDamageTypes() {
    }

    public static DamageSource bleeding(ServerPlayer player) {
        Registry<DamageType> registry = player.serverLevel()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(BLEEDING));
    }

    public static boolean isInternal(DamageSource source) {
        return source.is(BLEEDING) || source.is(INTERNAL_BLEEDING);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, path)
        );
    }
}
