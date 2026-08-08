package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.damage.BloodLossDamage;
import com.swampd.superficialtrauma.common.damage.ShotgunVolleyAggregator;
import com.swampd.superficialtrauma.common.entity.CorpseService;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import com.swampd.superficialtrauma.common.loot.LootingService;
import com.swampd.superficialtrauma.common.treatment.DefibrillationService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BodyStateEvents {
    private static final int INFECTION_NAUSEA_REFRESH_DURATION_TICKS = 5 * 20;
    private static final UUID NECROSIS_MAX_HEALTH_MODIFIER_ID = UUID.fromString(
            "fbd950e2-518e-4a48-a23e-ef19b1973d6c"
    );
    private static final String NECROSIS_MAX_HEALTH_MODIFIER_NAME = "Superficial Trauma necrosis";
    private static final UUID AWAKENING_RECOVERY_SPEED_MODIFIER_ID = UUID.fromString(
            "af5dd27f-a30b-43c4-9867-a02820f431dd"
    );
    private static final String AWAKENING_RECOVERY_SPEED_MODIFIER_NAME =
            "Superficial Trauma awakening recovery";
    private static final double AWAKENING_RECOVERY_SPEED_MULTIPLIER = -0.75D;

    private BodyStateEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            BodyStateProvider provider = new BodyStateProvider();
            event.addCapability(BodyStateCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ShotgunVolleyAggregator.clearPlayer(event.getOriginal().getUUID());
        GiveUpService.forgetPlayer(event.getOriginal().getUUID());
        event.getOriginal().reviveCaps();
        BodyStateCapability.get(event.getOriginal()).ifPresent(oldState ->
                BodyStateCapability.get(event.getEntity()).ifPresent(newState -> {
                    if (event.isWasDeath()) {
                        newState.copyPersistentKnowledgeFrom(oldState);
                    } else {
                        newState.copyFrom(oldState);
                    }
                })
        );
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        LootingService.forgetPlayer(event.getEntity().getUUID());
        AutopsyService.forgetPlayer(event.getEntity().getUUID());
        GiveUpService.forgetPlayer(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BodyStateCapability.get(serverPlayer).ifPresent(bodyState ->
                    bodyState.pauseBodyProgression(serverPlayer.serverLevel().getGameTime())
            );
        }
        ModNetworking.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver
                && event.getTarget() instanceof ServerPlayer subject) {
            ensureDownedPoseSnapshot(subject);
            ModNetworking.syncDownedPoseTo(subject, receiver);
            if (DefibrillationService.isCharging(subject.getUUID())) {
                ModNetworking.syncDefibrillatorChargingTo(subject, receiver, true);
            }
        }
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver
                && event.getTarget() instanceof ServerPlayer subject) {
            ModNetworking.clearDownedPoseFor(receiver, subject.getId());
            ModNetworking.syncDefibrillatorChargingTo(subject, receiver, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        long gameTime = serverPlayer.serverLevel().getGameTime();
        LootingService.closeIfInvalid(serverPlayer);
        AutopsyService.tick(serverPlayer);
        GiveUpService.tick(serverPlayer);
        BodyStateCapability.get(serverPlayer).ifPresent(bodyState -> {
            boolean shotgunVolleyResolved = ShotgunVolleyAggregator.resolveReady(
                    serverPlayer,
                    bodyState,
                    gameTime
            );
            boolean traumaticMovement = serverPlayer.isSprinting()
                    || serverPlayer.getDeltaMovement().y > 0.08D;
            BodyProgressionResult result = bodyState.advanceBodyProgression(
                    gameTime,
                    traumaticMovement,
                    serverPlayer.getFoodData().getFoodLevel()
            );
            InfusionProgression infusion = bodyState.advanceInfusion(gameTime);
            if (infusion.healingAmount() > 0.0F && serverPlayer.isAlive()) {
                serverPlayer.setHealth(Math.min(
                        serverPlayer.getMaxHealth(),
                        serverPlayer.getHealth() + infusion.healingAmount()
                ));
            }
            if (result.bleedingDamage() > 0.0F && serverPlayer.isAlive()) {
                BloodLossDamage.apply(serverPlayer, result.bleedingDamage());
            }
            AwakeningProgression awakening = bodyState.advanceAwakening(serverPlayer.getHealth(), gameTime);
            updateAwakeningRecoverySpeed(serverPlayer, bodyState, gameTime);
            updateInfectionEffects(serverPlayer, bodyState, gameTime);
            updateNecrosisEffects(serverPlayer, bodyState, gameTime);
            boolean poseCaptured = !bodyState.canAct()
                    && bodyState.captureDownedPose(DownedPoseCapture.capture(serverPlayer, null, gameTime));
            if (awakening.completed()) {
                DownedHitbox.restore(serverPlayer);
            } else {
                DownedHitbox.update(serverPlayer, bodyState);
            }
            notifyShockState(serverPlayer, bodyState, result, gameTime);
            if (bodyState.lifeState() == BodyLifeState.BRAIN_DEAD) {
                if (result.changed() || infusion.changed() || awakening.changed() || poseCaptured || shotgunVolleyResolved) {
                    ModNetworking.syncBodyState(serverPlayer);
                }
                if (poseCaptured) {
                    ModNetworking.syncDownedPose(serverPlayer);
                }
                triggerTrueDeath(serverPlayer, bodyState);
                return;
            }
            enforceIncapacitation(serverPlayer, bodyState);
            if (result.changed() || infusion.changed() || awakening.changed() || poseCaptured || shotgunVolleyResolved) {
                ModNetworking.syncBodyState(serverPlayer);
            }
            if (poseCaptured || awakening.completed()) {
                ModNetworking.syncDownedPose(serverPlayer);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            float multiplier = bodyState.vanillaHealingMultiplier();
            if (multiplier < 1.0F) {
                event.setAmount(event.getAmount() * multiplier);
            }
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ShotgunVolleyAggregator.clearAll();
        LootingService.clearAll();
        AutopsyService.clearAll();
        GiveUpService.clearAll();
    }

    private static void triggerTrueDeath(ServerPlayer player, BodyState bodyState) {
        if (!player.isAlive()) {
            return;
        }
        CorpseService.spawn(player, bodyState);
        player.setHealth(0.0F);
        player.die(player.damageSources().genericKill());
    }

    private static void notifyShockState(
            ServerPlayer player,
            BodyState bodyState,
            BodyProgressionResult result,
            long gameTime
    ) {
        if (result.becameIncapacitated()) {
            String messageKey = bodyState.collapseReason() == CollapseReason.SEPSIS
                    ? "message.superficialtrauma.sepsis_incapacitated"
                    : "message.superficialtrauma.traumatic_shock_incapacitated";
            player.displayClientMessage(
                    Component.translatable(messageKey),
                    true
            );
            return;
        }
        if (bodyState.isShockWarningActive(gameTime)
                && (result.shockWarningStarted() || gameTime % 20L == 0L)) {
            player.displayClientMessage(
                    Component.translatable("message.superficialtrauma.shock_warning"),
                    true
            );
        }
    }

    private static void updateInfectionEffects(ServerPlayer player, BodyState bodyState, long gameTime) {
        if ((bodyState.hasInfectionNausea() || bodyState.hasDrugNausea()) && gameTime % 20L == 0L) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION,
                    INFECTION_NAUSEA_REFRESH_DURATION_TICKS,
                    0,
                    false,
                    false,
                    true
            ));
        }
    }

    private static void updateNecrosisEffects(ServerPlayer player, BodyState bodyState, long gameTime) {
        AttributeInstance maximumHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth != null) {
            double requiredReduction = bodyState.necrosisMaximumHealthReduction();
            AttributeModifier existing = maximumHealth.getModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID);
            double requiredAmount = -requiredReduction;
            if (existing != null && Math.abs(existing.getAmount() - requiredAmount) > 0.0001D) {
                maximumHealth.removeModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID);
                existing = null;
            }
            if (requiredReduction <= 0.0D) {
                if (existing != null) {
                    maximumHealth.removeModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID);
                }
            } else if (existing == null) {
                maximumHealth.addTransientModifier(new AttributeModifier(
                        NECROSIS_MAX_HEALTH_MODIFIER_ID,
                        NECROSIS_MAX_HEALTH_MODIFIER_NAME,
                        requiredAmount,
                        AttributeModifier.Operation.ADDITION
                ));
            }
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        if (bodyState.hasNecrosisSlowness() && gameTime % 20L == 0L) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    3 * 20,
                    0,
                    false,
                    false,
                    true
            ));
        }
    }

    private static void updateAwakeningRecoverySpeed(
            ServerPlayer player,
            BodyState bodyState,
            long gameTime
    ) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        AttributeModifier existing = movementSpeed.getModifier(AWAKENING_RECOVERY_SPEED_MODIFIER_ID);
        if (!bodyState.isAwakeningRecoveryActive(gameTime)) {
            if (existing != null) {
                movementSpeed.removeModifier(AWAKENING_RECOVERY_SPEED_MODIFIER_ID);
            }
            return;
        }
        if (existing == null) {
            movementSpeed.addTransientModifier(new AttributeModifier(
                    AWAKENING_RECOVERY_SPEED_MODIFIER_ID,
                    AWAKENING_RECOVERY_SPEED_MODIFIER_NAME,
                    AWAKENING_RECOVERY_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    public static void clearNecrosisHealthModifier(ServerPlayer player) {
        AttributeInstance maximumHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth != null && maximumHealth.getModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID) != null) {
            maximumHealth.removeModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID);
        }
    }

    private static void enforceIncapacitation(ServerPlayer player, BodyState bodyState) {
        if (bodyState.canAct()) {
            return;
        }

        player.setSprinting(false);
        player.setShiftKeyDown(false);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
    }

    private static void syncIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ensureDownedPoseSnapshot(serverPlayer);
            ModNetworking.syncBodyState(serverPlayer);
            ModNetworking.syncDownedPose(serverPlayer);
        }
    }

    private static void ensureDownedPoseSnapshot(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            if (!bodyState.canAct()) {
                bodyState.captureDownedPose(DownedPoseCapture.capture(player, null, gameTime));
                DownedHitbox.update(player, bodyState);
            }
        });
    }

    private static void resumeProgressionIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            BodyStateCapability.get(serverPlayer).ifPresent(bodyState ->
                    bodyState.resumeBodyProgression(serverPlayer.serverLevel().getGameTime())
            );
        }
    }
}
