package com.swampd.superficialtrauma.common.body;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One owned effect, with vanilla-equivalent Slowness/Weakness attributes; never removes external potions. */
public final class FatigueEffect extends MobEffect {
    private static final String SPEED_ID = "d2104543-6dd3-493d-b724-f2537c80ec56";
    private static final String DAMAGE_ID = "c620dd2a-f629-4df1-a1c2-086ab3b02de8";

    public FatigueEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B9872);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_ID, -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_ID, -4.0D, AttributeModifier.Operation.ADDITION);
    }

    public static int slownessLevel(int fatigue) {
        return (Math.max(0, Math.min(4, fatigue)) + 1) / 2;
    }

    public static int weaknessLevel(int fatigue) {
        return Math.max(0, Math.min(4, fatigue)) / 2;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getId().toString().equals(SPEED_ID)
                ? -0.15D * slownessLevel(amplifier + 1)
                : -4.0D * weaknessLevel(amplifier + 1);
    }
}
