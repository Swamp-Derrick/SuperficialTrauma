package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.damage.BloodLossDamage;
import com.swampd.superficialtrauma.common.damage.ShotgunVolleyAggregator;
import com.swampd.superficialtrauma.common.entity.CorpseService;
import com.swampd.superficialtrauma.common.drag.BodyDragService;
import com.swampd.superficialtrauma.common.drag.BodyRotationService;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import com.swampd.superficialtrauma.common.loot.LootingService;
import com.swampd.superficialtrauma.common.qte.TimingQteService;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class BodyStateEvents {
    private static final Map<UUID, Vec3> LAST_WOUND_POSITIONS = new HashMap<>();
    private static final int INFECTION_NAUSEA_REFRESH_DURATION_TICKS = 5 * 20;
    private static final int EPINEPHRINE_EFFECT_REFRESH_DURATION_TICKS = 15;
    private static final int ORGANOPHOSPHATE_EFFECT_REFRESH_DURATION_TICKS = 5 * 20;
    private static final net.minecraft.resources.ResourceLocation NECROSIS_MAX_HEALTH_MODIFIER_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "necrosis_max_health_modifier_id");
    private static final String NECROSIS_MAX_HEALTH_MODIFIER_NAME = "Superficial Trauma necrosis";
    private static final net.minecraft.resources.ResourceLocation AWAKENING_RECOVERY_SPEED_MODIFIER_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "awakening_recovery_speed_modifier_id");
    private static final String AWAKENING_RECOVERY_SPEED_MODIFIER_NAME =
            "Superficial Trauma awakening recovery";
    private static final double AWAKENING_RECOVERY_SPEED_MULTIPLIER = -0.75D;

    private BodyStateEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        LAST_WOUND_POSITIONS.remove(event.getOriginal().getUUID());
        ShotgunVolleyAggregator.clearPlayer(event.getOriginal().getUUID());
        GiveUpService.forgetPlayer(event.getOriginal().getUUID());
        BodyDragService.forgetPlayer(event.getOriginal().getUUID());
        BodyRotationService.forgetPlayer(event.getOriginal().getUUID());
        BodyStateCapability.get(event.getOriginal()).ifPresent(oldState ->
                BodyStateCapability.get(event.getEntity()).ifPresent(newState -> {
                    if (event.isWasDeath()) {
                        newState.copyPersistentKnowledgeFrom(oldState);
                    } else {
                        newState.copyFrom(oldState);
                    }
                })
        );
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LAST_WOUND_POSITIONS.remove(event.getEntity().getUUID());
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_WOUND_POSITIONS.remove(event.getEntity().getUUID());
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        LootingService.forgetPlayer(event.getEntity().getUUID());
        AutopsyService.forgetPlayer(event.getEntity().getUUID());
        GiveUpService.forgetPlayer(event.getEntity().getUUID());
        BodyDragService.forgetPlayer(event.getEntity().getUUID());
        BodyRotationService.forgetPlayer(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BodyStateCapability.get(serverPlayer).ifPresent(bodyState -> {
                bodyState.cancelInfusion();
                bodyState.pauseBodyProgression(serverPlayer.serverLevel().getGameTime());
            });
        }
        ModNetworking.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST_WOUND_POSITIONS.remove(event.getEntity().getUUID());
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        BodyDragService.forgetPlayer(event.getEntity().getUUID());
        BodyRotationService.forgetPlayer(event.getEntity().getUUID());
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_WOUND_POSITIONS.remove(event.getEntity().getUUID());
        ShotgunVolleyAggregator.clearPlayer(event.getEntity().getUUID());
        BodyDragService.forgetPlayer(event.getEntity().getUUID());
        BodyRotationService.forgetPlayer(event.getEntity().getUUID());
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver) {
            BodyDragService.syncForTracking(event.getTarget(), receiver);
        }
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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        long gameTime = serverPlayer.serverLevel().getGameTime();
        BodyDragService.tick(serverPlayer);
        BodyRotationService.tick(serverPlayer);
        LootingService.closeIfInvalid(serverPlayer);
        TimingQteService.tick(serverPlayer);
        AutopsyService.tick(serverPlayer);
        GiveUpService.tick(serverPlayer);
        // Network-driven player movement need not leave a horizontal deltaMovement;
        // entity xo/yo/zo can also be reset before this END-phase event.
        Vec3 previousPosition = LAST_WOUND_POSITIONS.put(serverPlayer.getUUID(), serverPlayer.position());
        Vec3 woundDisplacement = previousPosition == null ? Vec3.ZERO : serverPlayer.position().subtract(previousPosition);
        BodyStateCapability.get(serverPlayer).ifPresent(bodyState -> {
            boolean shotgunVolleyResolved = ShotgunVolleyAggregator.resolveReady(
                    serverPlayer,
                    bodyState,
                    gameTime
            );
            boolean traumaticMovement = WoundMovementRules.isMoving(
                    woundDisplacement.x,
                    woundDisplacement.y,
                    woundDisplacement.z,
                    serverPlayer.getDeltaMovement().horizontalDistanceSqr(),
                    serverPlayer.getDeltaMovement().y,
                    serverPlayer.isSprinting()
            );
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
            updateEpinephrineEffects(serverPlayer, bodyState, gameTime);
            updateOrganophosphateEffects(serverPlayer, bodyState, gameTime);
            updateInfectionEffects(serverPlayer, bodyState, gameTime);
            updateNecrosisEffects(serverPlayer, bodyState, gameTime);
            updateFatigueEffect(serverPlayer, bodyState);
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
        LAST_WOUND_POSITIONS.clear();
        ShotgunVolleyAggregator.clearAll();
        LootingService.clearAll();
        AutopsyService.clearAll();
        GiveUpService.clearAll();
        BodyDragService.clearAll();
        BodyRotationService.clearAll();
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
            String messageKey = switch (bodyState.collapseReason()) {
                case SEPSIS -> "message.superficialtrauma.sepsis_incapacitated";
                case TRAUMATIC_SHOCK -> "message.superficialtrauma.traumatic_shock_incapacitated";
                default -> null;
            };
            if (messageKey == null) {
                return;
            }
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

    private static void updateEpinephrineEffects(ServerPlayer player, BodyState bodyState, long gameTime) {
        if (!bodyState.canAct() || gameTime % 10L != 0L) {
            return;
        }
        int activeLayers = bodyState.activeEpinephrineDoseCount();
        if (activeLayers <= 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                EPINEPHRINE_EFFECT_REFRESH_DURATION_TICKS,
                activeLayers - 1,
                false,
                false,
                true
        ));

        int speedLayers = bodyState.activeEpinephrineSpeedDoseCount();
        if (speedLayers > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    EPINEPHRINE_EFFECT_REFRESH_DURATION_TICKS,
                    speedLayers - 1,
                    false,
                    false,
                    true
            ));
        }
    }

    private static void updateOrganophosphateEffects(
            ServerPlayer player,
            BodyState bodyState,
            long gameTime
    ) {
        if (!bodyState.hasActiveOrganophosphateSymptoms() || gameTime % 10L != 0L) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                ORGANOPHOSPHATE_EFFECT_REFRESH_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
    }

    private static void updateNecrosisEffects(ServerPlayer player, BodyState bodyState, long gameTime) {
        AttributeInstance maximumHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth != null) {
            double requiredReduction = bodyState.necrosisMaximumHealthReduction();
            AttributeModifier existing = maximumHealth.getModifier(NECROSIS_MAX_HEALTH_MODIFIER_ID);
            double requiredAmount = -requiredReduction;
            if (existing != null && Math.abs(existing.amount() - requiredAmount) > 0.0001D) {
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
                        requiredAmount,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

    }

    private static void updateFatigueEffect(ServerPlayer player, BodyState state) {
        var effect = com.swampd.superficialtrauma.common.init.ModEffects.FATIGUE;
        int level = state.lifeState() == BodyLifeState.BRAIN_DEAD ? 0 : state.fatigueLevel();
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && (level == 0 || existing.getAmplifier() != level - 1)) {
            player.removeEffect(effect);
            existing = null;
        }
        if (level > 0 && (existing == null || existing.getDuration() <= 20)) {
            player.addEffect(new MobEffectInstance(effect, 40, level - 1, false, false, false));
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
                    AWAKENING_RECOVERY_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
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
        Vec3 pull = BodyDragService.pullMovement(player);
        double verticalMovement = BodyDragService.bodyVerticalMovement(player, movement.y, pull.y);
        player.setDeltaMovement(pull.x, verticalMovement, pull.z);
        if (BodyDragService.isBeingDragged(player) || player.isInWaterOrBubble()) {
            player.hurtMarked = true;
        }
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
