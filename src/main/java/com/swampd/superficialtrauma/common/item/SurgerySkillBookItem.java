package com.swampd.superficialtrauma.common.item;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SurgerySkillBookItem extends Item {
    public SurgerySkillBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        boolean learned = BodyStateCapability.get(serverPlayer)
                .map(bodyState -> bodyState.unlockSurgerySkill())
                .orElse(false);
        if (!learned) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.superficialtrauma.surgery_skill.already_known"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        stack.shrink(1);
        serverPlayer.displayClientMessage(
                Component.translatable("message.superficialtrauma.surgery_skill.learned"),
                true
        );
        ModNetworking.syncBodyState(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
