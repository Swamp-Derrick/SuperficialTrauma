package com.swampd.superficialtrauma.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class DefibrillatorItem extends Item {
    public static final int MAX_ENERGY = 900;
    private static final String TAG_ENERGY = "Energy";

    public DefibrillatorItem(Properties properties) {
        super(properties);
    }

    public static int getEnergy(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (!stack.hasTag() || !stack.getTag().contains(TAG_ENERGY)) {
            return MAX_ENERGY;
        }
        return Math.max(0, Math.min(MAX_ENERGY, stack.getTag().getInt(TAG_ENERGY)));
    }

    public static boolean consumeEnergy(ItemStack stack, int amount) {
        int required = Math.max(0, amount);
        int current = getEnergy(stack);
        if (current < required) {
            return false;
        }
        stack.getOrCreateTag().putInt(TAG_ENERGY, current - required);
        return true;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt(TAG_ENERGY, Math.max(0, Math.min(MAX_ENERGY, energy)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergy(stack) < MAX_ENERGY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fraction = getEnergy(stack) / (float) MAX_ENERGY;
        return net.minecraft.util.Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(
                "item.superficialtrauma.defibrillator.energy",
                getEnergy(stack),
                MAX_ENERGY
        ).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
