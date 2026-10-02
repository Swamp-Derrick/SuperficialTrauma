package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
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
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player && cannotAct(player)) {
            event.setCanceled(true);
        }
    }

    private static void onPlayerInteract(PlayerInteractEvent event) {
        if (cannotAct(event.getEntity()) && event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    @SubscribeEvent public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) { onPlayerInteract(event); }
    @SubscribeEvent public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) { onPlayerInteract(event); }
    @SubscribeEvent public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) { onPlayerInteract(event); }
    @SubscribeEvent public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) { onPlayerInteract(event); }
    @SubscribeEvent public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) { onPlayerInteract(event); }

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

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!cannotAct(event.getPlayer())) {
            return;
        }

        ItemStack returnedStack = event.getEntity().getItem().copy();
        event.getPlayer().getInventory().add(returnedStack);
        if (!returnedStack.isEmpty()) {
            event.getEntity().setItem(returnedStack);
            SuperficialTrauma.LOGGER.error(
                    "Only partially restored blocked downed item toss for player {}; dropping the remainder safely",
                    event.getPlayer().getGameProfile().getName()
            );
            return;
        }
        event.getPlayer().containerMenu.broadcastChanges();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof ServerPlayer player && cannotAct(player)) {
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
