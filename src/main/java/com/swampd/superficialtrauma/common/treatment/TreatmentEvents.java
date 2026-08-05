package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TreatmentEvents {
    private static final String CGM_FIRE_PRE = "com.mrcrayfish.guns.event.GunFireEvent$Pre";
    private static final String CGM_RELOAD_PRE = "com.mrcrayfish.guns.event.GunReloadEvent$Pre";

    private TreatmentEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            AirwayService.tick(player);
            TreatmentService.tick(player);
            InspectionService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
            TreatmentService.cancelInvolving(player, TreatmentCancelReason.DAMAGE);
            AirwayService.cancelInvolving(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        cancelAction(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent event) {
        cancelAction(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelAction(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        cancelAction(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTossItem(ItemTossEvent event) {
        cancelAction(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelAction(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOptionalGunAction(PlayerEvent event) {
        String eventClass = event.getClass().getName();
        if ((CGM_FIRE_PRE.equals(eventClass) || CGM_RELOAD_PRE.equals(eventClass))
                && event.getEntity() instanceof ServerPlayer player) {
            cancelAction(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TreatmentService.cancelInvolving(player, TreatmentCancelReason.DISCONNECTED);
            AirwayService.cancelInvolving(player);
            InspectionService.forgetPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TreatmentService.clearAll();
        AirwayService.clearAll();
        InspectionService.clearAll();
    }

    private static void cancelAction(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TreatmentService.cancelInvolving(serverPlayer, TreatmentCancelReason.ACTION);
            AirwayService.cancelInvolving(serverPlayer);
        }
    }
}
