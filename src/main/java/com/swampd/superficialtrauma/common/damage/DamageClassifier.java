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
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return DamageClassification.explosion("explosion");
        }

        Optional<CgmProjectileContext> cgmContext = CgmProjectileContext.inspect(source);
        if (cgmContext.isPresent()) {
            CgmProjectileContext context = cgmContext.get();
            DamageKind kind = CgmAmmoTags.classify(context.ammoStack());
            String reason = kind == DamageKind.CGM_UNCLASSIFIED
                    ? "cgm_projectile_unknown_ammo"
                    : "cgm_projectile_ammo_tag";
            return DamageClassification.cgmProjectile(kind, reason, context);
        }

        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LIGHTNING_BOLT)) {
            return DamageClassification.burn("fire_lava_or_lightning");
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

        if (source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.STING)
                || source.is(DamageTypes.THORNS)) {
            return DamageClassification.deferred("minor_environmental_puncture_ignored");
        }

        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return DamageClassification.frostbite("freezing");
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.STALAGMITE)
                || source.is(DamageTypes.FALLING_STALACTITE)) {
            return DamageClassification.puncture("projectile_or_impalement");
        }

        if (source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_ANVIL)) {
            return DamageClassification.crush("compression_or_impact");
        }

        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof SwordItem
                    || weapon.getItem() instanceof AxeItem
                    || weapon.is(DamageItemTags.SHARP_WEAPONS)) {
                return DamageClassification.sharp("sharp_weapon");
            }
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            return DamageClassification.blunt("fall");
        }
        if (source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.is(DamageTypes.PLAYER_ATTACK)) {
            return DamageClassification.blunt("unarmed_or_blunt_melee");
        }
        return DamageClassification.unknown("unclassified_damage");
    }
}
