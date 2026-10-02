package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.medication.MedicationCancelReason;
import com.swampd.superficialtrauma.common.medication.MedicationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class TreatmentEvents {
    private static final String CGM_FIRE_PRE = "com.mrcrayfish.guns.event.GunFireEvent$Pre";
    private static final String CGM_RELOAD_PRE = "com.mrcrayfish.guns.event.GunReloadEvent$Pre";
    private static final net.minecraft.resources.ResourceLocation SELF_TREATMENT_SPEED_MODIFIER_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "self_treatment_speed_modifier_id");
    private static final String SELF_TREATMENT_SPEED_MODIFIER_NAME =
            "Superficial Trauma self treatment";
    private static final double SELF_TREATMENT_SPEED_MULTIPLIER = -0.5D;

    private TreatmentEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CprService.tick(player);
            DefibrillationService.tick(player);
            AirwayService.tick(player);
            TreatmentPreparationSoundService.tick(player);
            TreatmentService.tick(player);
            MedicationService.tick(player);
            updateSelfTreatmentSpeed(player);
            InspectionService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
            cancelForDamage(player);
        }
    }

    public static void cancelForDamage(ServerPlayer player) {
        TreatmentService.cancelInvolving(player, TreatmentCancelReason.DAMAGE);
        TreatmentPreparationSoundService.cancelInvolving(player);
        MedicationService.cancelInvolving(player, MedicationCancelReason.DAMAGE);
        AirwayService.cancelInvolving(player);
        CprService.cancelInvolving(player);
        DefibrillationService.cancelInvolving(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        cancelAction(event.getEntity());
    }

    private static void onInteract(PlayerInteractEvent event) {
        cancelAction(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) { onInteract(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) { onInteract(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) { onInteract(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) { onInteract(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) { onInteract(event); }

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

    public static void onOptionalGunAction(PlayerEvent event) {
        String eventClass = event.getClass().getName();
        if ((CGM_FIRE_PRE.equals(eventClass) || CGM_RELOAD_PRE.equals(eventClass))
                && event.getEntity() instanceof ServerPlayer player) {
            cancelAction(player);
        }
    }

    /** NeoForge does not allow subscribers on abstract PlayerEvent; register only the actual optional events. */
    public static void registerOptionalGunEvents() {
        for (String eventName : new String[] {CGM_FIRE_PRE, CGM_RELOAD_PRE}) {
            try {
                Class<? extends PlayerEvent> eventType = Class.forName(eventName).asSubclass(PlayerEvent.class);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                        EventPriority.HIGHEST, false, eventType, TreatmentEvents::onOptionalGunAction);
            } catch (ClassNotFoundException ignored) {
                // CGM is optional; never link its classes on a standalone installation.
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TreatmentService.cancelInvolving(player, TreatmentCancelReason.DISCONNECTED);
            TreatmentPreparationSoundService.cancelInvolving(player);
            MedicationService.cancelInvolving(player, MedicationCancelReason.DISCONNECTED);
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
        MedicationService.clearAll();
        AirwayService.clearAll();
        CprService.clearAll();
        DefibrillationService.clearAll();
        InspectionService.clearAll();
    }

    private static void cancelAction(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TreatmentService.cancelInvolving(serverPlayer, TreatmentCancelReason.ACTION);
            TreatmentPreparationSoundService.cancelInvolving(serverPlayer);
            MedicationService.cancelInvolving(serverPlayer, MedicationCancelReason.ACTION);
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
                    SELF_TREATMENT_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
