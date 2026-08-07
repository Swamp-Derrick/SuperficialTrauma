package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TreatmentEvents {
    private static final String CGM_FIRE_PRE = "com.mrcrayfish.guns.event.GunFireEvent$Pre";
    private static final String CGM_RELOAD_PRE = "com.mrcrayfish.guns.event.GunReloadEvent$Pre";
    private static final UUID SELF_TREATMENT_SPEED_MODIFIER_ID = UUID.fromString(
            "a4ad1c38-c243-420e-a029-604cb71e3a2d"
    );
    private static final String SELF_TREATMENT_SPEED_MODIFIER_NAME =
            "Superficial Trauma self treatment";
    private static final double SELF_TREATMENT_SPEED_MULTIPLIER = -0.5D;

    private TreatmentEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            CprService.tick(player);
            DefibrillationService.tick(player);
            AirwayService.tick(player);
            TreatmentPreparationSoundService.tick(player);
            TreatmentService.tick(player);
            updateSelfTreatmentSpeed(player);
            InspectionService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
            TreatmentService.cancelInvolving(player, TreatmentCancelReason.DAMAGE);
            TreatmentPreparationSoundService.cancelInvolving(player);
            AirwayService.cancelInvolving(player);
            CprService.cancelInvolving(player);
            DefibrillationService.cancelInvolving(player);
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
            TreatmentPreparationSoundService.cancelInvolving(player);
            AirwayService.cancelInvolving(player);
            CprService.cancelInvolving(player);
            DefibrillationService.cancelInvolving(player);
            InspectionService.forgetPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TreatmentService.clearAll();
        TreatmentPreparationSoundService.clearAll();
        AirwayService.clearAll();
        CprService.clearAll();
        DefibrillationService.clearAll();
        InspectionService.clearAll();
    }

    private static void cancelAction(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TreatmentService.cancelInvolving(serverPlayer, TreatmentCancelReason.ACTION);
            TreatmentPreparationSoundService.cancelInvolving(serverPlayer);
            AirwayService.cancelInvolving(serverPlayer);
            CprService.cancelInvolving(serverPlayer);
            DefibrillationService.cancelInvolving(serverPlayer);
        }
    }

    private static void updateSelfTreatmentSpeed(ServerPlayer player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        AttributeModifier existing = movementSpeed.getModifier(SELF_TREATMENT_SPEED_MODIFIER_ID);
        if (!TreatmentService.isSelfTreating(player.getUUID())) {
            if (existing != null) {
                movementSpeed.removeModifier(SELF_TREATMENT_SPEED_MODIFIER_ID);
            }
            return;
        }
        if (existing == null) {
            movementSpeed.addTransientModifier(new AttributeModifier(
                    SELF_TREATMENT_SPEED_MODIFIER_ID,
                    SELF_TREATMENT_SPEED_MODIFIER_NAME,
                    SELF_TREATMENT_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}
