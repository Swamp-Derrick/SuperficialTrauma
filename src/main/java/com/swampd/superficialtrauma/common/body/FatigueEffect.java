package com.swampd.superficialtrauma.common.body;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One owned effect, with vanilla-equivalent Slowness/Weakness attributes; never removes external potions. */
public final class FatigueEffect extends MobEffect {
    private static final net.minecraft.resources.ResourceLocation SPEED_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("superficialtrauma", "fatigue_speed");
    private static final net.minecraft.resources.ResourceLocation DAMAGE_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("superficialtrauma", "fatigue_damage");

    public FatigueEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B9872);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_ID, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> -0.15D * slownessLevel(amplifier + 1));
        addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_ID, AttributeModifier.Operation.ADD_VALUE,
                amplifier -> -4.0D * weaknessLevel(amplifier + 1));
    }

    public static int slownessLevel(int fatigue) {
        return (Math.max(0, Math.min(4, fatigue)) + 1) / 2;
    }

    public static int weaknessLevel(int fatigue) {
        return Math.max(0, Math.min(4, fatigue)) / 2;
    }

}
