package com.swampd.superficialtrauma.common.damage;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.Optional;

public final class DamageClassifier {
    private DamageClassifier() {
    }

    public static DamageClassification classify(Player patient, DamageSource source) {
        Optional<CgmProjectileContext> cgmContext = CgmProjectileContext.inspect(source);
        if (cgmContext.isPresent()) {
            CgmProjectileContext context = cgmContext.get();
            DamageKind kind = CgmAmmoTags.classify(context.ammoStack());
            String reason = kind == DamageKind.CGM_UNCLASSIFIED
                    ? "cgm_projectile_unknown_ammo"
                    : "cgm_projectile_ammo_tag";
            return DamageClassification.cgmProjectile(kind, reason, context);
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            return DamageClassification.deferred("burn_not_implemented");
        }
        if (source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.OUTSIDE_BORDER)
                || source.is(DamageTypes.GENERIC_KILL)) {
            return DamageClassification.deferred("non_traumatic_damage");
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE) && patient.getArmorValue() <= 0) {
            return DamageClassification.deferred("unarmored_projectile_not_implemented");
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem) {
                return DamageClassification.deferred("sharp_wound_not_implemented");
            }
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            return DamageClassification.blunt("fall");
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return DamageClassification.blunt("armored_projectile");
        }
        return DamageClassification.blunt("blunt_or_fallback");
    }
}
