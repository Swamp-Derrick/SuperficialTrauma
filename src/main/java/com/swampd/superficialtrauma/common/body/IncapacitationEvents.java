package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncapacitationEvents {
    private IncapacitationEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (cannotAct(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player && cannotAct(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (cannotAct(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && cannotAct(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (cannotAct(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static boolean cannotAct(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer)) {
            return false;
        }
        return BodyStateCapability.get(player)
                .map(bodyState -> !bodyState.canAct())
                .orElse(false);
    }
}
