package com.swampd.superficialtrauma.common.forensics;

import com.swampd.superficialtrauma.common.damage.CgmProjectileContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Name only, captured from the bullet's weapon, never the shooter's possibly changed hand. */
public final class WeaponNameSnapshot {
    public static final int MAX_JSON_LENGTH = 16_384;

    private WeaponNameSnapshot() { }

    public static String capture(DamageSource source) {
        if (source.getDirectEntity() == null) return "";
        return CgmProjectileContext.inspect(source)
                .map(context -> capture(context.weaponStack(), source.getDirectEntity().registryAccess()))
                .orElse("");
    }

    public static String capture(ItemStack weapon, HolderLookup.Provider registries) {
        if (weapon.isEmpty()) return "";
        Component name = weapon.getHoverName();
        String json = Component.Serializer.toJson(name, registries);
        if (json.length() <= MAX_JSON_LENGTH) return json;
        String plain = name.getString();
        return Component.Serializer.toJson(Component.literal(plain.substring(0, Math.min(256, plain.length()))), registries);
    }

    public static String normalize(String json) {
        return json == null || json.length() > MAX_JSON_LENGTH ? "" : json;
    }

    public static Optional<Component> restore(String json, HolderLookup.Provider registries) {
        if (json == null || json.isBlank() || json.length() > MAX_JSON_LENGTH) return Optional.empty();
        try {
            return Optional.ofNullable(Component.Serializer.fromJson(json, registries));
        } catch (RuntimeException malformedName) {
            return Optional.empty();
        }
    }
}
