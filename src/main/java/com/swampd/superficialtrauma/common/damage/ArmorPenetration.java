package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** ST wound routing only. Never changes vanilla damage, enchantments or equipment durability. */
public final class ArmorPenetration {
    public static final double STRONG_ARMOR_RATING = 10.0;
    public static final double CLOSE_SHOTGUN_DISTANCE = 3.0;

    private ArmorPenetration() { }

    public static boolean penetrates(double power, double armorRating) {
        return Double.isFinite(power) && Double.isFinite(armorRating)
                && power >= 0 && power + 1.0E-7 >= Math.max(0, armorRating);
    }

    public static double bulletPower(WoundType type, double distance) {
        return switch (type) {
            case GUNSHOT_LOW_VELOCITY -> 5;
            case GUNSHOT_HIGH_VELOCITY -> 13;
            case GUNSHOT_SHOTGUN -> distance >= 0 && distance <= CLOSE_SHOTGUN_DISTANCE ? 8 : 5;
            default -> throw new IllegalArgumentException("Not a gunshot: " + type);
        };
    }

    public static WoundType bulletWound(DamageKind kind) {
        return switch (kind) {
            case CGM_LOW_VELOCITY -> WoundType.GUNSHOT_LOW_VELOCITY;
            case CGM_HIGH_VELOCITY -> WoundType.GUNSHOT_HIGH_VELOCITY;
            case CGM_SHOTGUN -> WoundType.GUNSHOT_SHOTGUN;
            default -> null;
        };
    }

    public static double projectilePower(DamageSource source) {
        if (source.is(DamageTypes.TRIDENT)) return 7;
        if (source.is(DamageTypes.ARROW)) return 4;
        return 2;
    }

    public static double meleePower(float preArmorDamage) {
        return Float.isFinite(preArmorDamage) ? Math.max(0, preArmorDamage) * 0.8 : 0;
    }

    public static DamageClassification routeNonBullet(DamageClassification original, DamageSource source,
            Profile armor, BulletHitLocation location, float preArmorDamage) {
        if (original.woundType() == WoundType.SHARP
                && !penetrates(meleePower(preArmorDamage), armor.overallRating())) {
            return new DamageClassification(WoundType.BLUNT, DamageKind.BLUNT, "sharp_stopped_by_armor",
                    original.projectileEntityId(), original.ammoId(), original.weaponId());
        }
        // Environmental impalement (stalagmites/stalactites) and fire/explosions keep their own rules.
        if (original.woundType() == WoundType.PUNCTURE && source.is(DamageTypeTags.IS_PROJECTILE)
                && !penetrates(projectilePower(source), armor.rating(location))) {
            return new DamageClassification(null, DamageKind.DEFERRED, "projectile_stopped_by_armor",
                    original.projectileEntityId(), original.ammoId(), original.weaponId());
        }
        return original;
    }

    /** Snapshot before the hit can break equipment; slot modifiers include modded armor components. */
    public static Profile snapshot(LivingEntity entity) {
        return new Profile(piece(entity, EquipmentSlot.HEAD), piece(entity, EquipmentSlot.CHEST),
                piece(entity, EquipmentSlot.LEGS), piece(entity, EquipmentSlot.FEET));
    }

    private static Piece piece(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return new Piece(attribute(stack, slot, Attributes.ARMOR), attribute(stack, slot, Attributes.ARMOR_TOUGHNESS));
    }

    private static double attribute(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
        AttributeInstance instance = new AttributeInstance(attribute, ignored -> { });
        instance.setBaseValue(0);
        // No enchantment damage-protection contribution to R. Respect equipment's actual attribute modifiers.
        stack.getAttributeModifiers().forEach(slot, (type, modifier) -> {
            if (type.equals(attribute)) instance.addOrReplacePermanentModifier(modifier);
        });
        return instance.getValue();
    }

    public record Piece(double armor, double toughness) {
        public static final Piece NONE = new Piece(0, 0);
        public Piece {
            armor = Double.isFinite(armor) ? Math.max(0, armor) : 0;
            toughness = Double.isFinite(toughness) ? Math.max(0, toughness) : 0;
        }
    }

    public record Profile(Piece head, Piece chest, Piece legs, Piece feet) {
        public double rating(BulletHitLocation location) {
            if (location == null) return overallRating();
            return switch (location) {
                case HEAD -> head.armor * (8.0 / 3.0) + head.toughness * 2;
                case CHEST -> chest.armor + chest.toughness * 2;
                case LIMBS -> (legs.armor + feet.armor) * (8.0 / 9.0) + legs.toughness + feet.toughness;
                case UNKNOWN -> overallRating();
            };
        }

        public double overallRating() {
            return (head.armor + chest.armor + legs.armor + feet.armor) * 0.4
                    + (head.toughness + chest.toughness + legs.toughness + feet.toughness) * 0.5;
        }
    }
}
