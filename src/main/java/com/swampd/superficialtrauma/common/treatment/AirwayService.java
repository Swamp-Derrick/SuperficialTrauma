package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AirwayService {
    private static final double MAX_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final long OXYGEN_PULSE_TICKS = 3L * 20L;
    private static final long PUBLISH_INTERVAL_TICKS = 20L;
    private static final Map<UUID, AirwaySession> SESSION_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_PATIENT = new HashMap<>();

    private AirwayService() {
    }

    public static boolean start(ServerPlayer actor, int patientEntityId) {
        if (!(actor.serverLevel().getEntity(patientEntityId) instanceof ServerPlayer patient)
                || !canContinue(actor, patient)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.airway.invalid_target"),
                    true
            );
            return false;
        }
        UUID existingActor = ACTOR_BY_PATIENT.get(patient.getUUID());
        if (existingActor != null && !existingActor.equals(actor.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.airway.patient_busy"),
                    true
            );
            return false;
        }
        stopActor(actor.getUUID());
        long gameTime = actor.serverLevel().getGameTime();
        AirwaySession session = new AirwaySession(patient.getUUID(), gameTime, gameTime);
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        return true;
    }

    public static void stop(ServerPlayer actor, int patientEntityId) {
        AirwaySession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || patient.getId() == patientEntityId) {
            stopActor(actor.getUUID());
        }
    }

    public static void tick(ServerPlayer actor) {
        AirwaySession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || !canContinue(actor, patient)) {
            stopActor(actor.getUUID());
            return;
        }

        long gameTime = actor.serverLevel().getGameTime();
        long elapsedTicks = Math.max(0L, gameTime - session.lastTickGameTime);
        session.lastTickGameTime = gameTime;
        session.continuousTicks += elapsedTicks;
        int completedOxygenPulses = (int) (session.continuousTicks / OXYGEN_PULSE_TICKS);
        session.continuousTicks %= OXYGEN_PULSE_TICKS;
        boolean publish = gameTime - session.lastPublishedGameTime >= PUBLISH_INTERVAL_TICKS
                || completedOxygenPulses > 0;

        BodyState bodyState = BodyStateCapability.get(patient).orElse(null);
        if (bodyState == null || !bodyState.advanceAssistedBreathing(
                elapsedTicks,
                completedOxygenPulses,
                gameTime,
                publish
        )) {
            stopActor(actor.getUUID());
            return;
        }
        if (completedOxygenPulses > 0) {
            bodyState.recordResuscitationContributor(
                    actor.getUUID(),
                    actor.getGameProfile().getName()
            );
        }
        if (publish) {
            session.lastPublishedGameTime = gameTime;
            ModNetworking.syncBodyState(patient);
            InspectionService.syncPatient(patient);
        }
    }

    public static void cancelInvolving(ServerPlayer player) {
        stopActor(player.getUUID());
        UUID actorId = ACTOR_BY_PATIENT.get(player.getUUID());
        if (actorId != null) {
            stopActor(actorId);
        }
    }

    public static void clearAll() {
        SESSION_BY_ACTOR.clear();
        ACTOR_BY_PATIENT.clear();
    }

    private static boolean canContinue(ServerPlayer actor, ServerPlayer patient) {
        if (actor == patient
                || !actor.isAlive()
                || actor.isRemoved()
                || actor.isSpectator()
                || !patient.isAlive()
                || patient.isRemoved()
                || actor.serverLevel() != patient.serverLevel()
                || actor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !actor.hasLineOfSight(patient)
                || !InspectionService.isInspecting(actor, patient.getId())
                || !hasManualResuscitator(actor)) {
            return false;
        }
        boolean actorCanAct = BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false);
        BodyLifeState patientState = BodyStateCapability.get(patient)
                .map(BodyState::lifeState)
                .orElse(BodyLifeState.ACTIVE);
        return actorCanAct
                && (patientState == BodyLifeState.INCAPACITATED || patientState == BodyLifeState.AWAKENING);
    }

    private static boolean hasManualResuscitator(ServerPlayer actor) {
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.MANUAL_RESUSCITATOR.get())) {
                return true;
            }
        }
        return false;
    }

    private static void stopActor(UUID actorId) {
        AirwaySession removed = SESSION_BY_ACTOR.remove(actorId);
        if (removed != null) {
            ACTOR_BY_PATIENT.remove(removed.patientId, actorId);
        }
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static final class AirwaySession {
        private final UUID patientId;
        private long lastTickGameTime;
        private long lastPublishedGameTime;
        private long continuousTicks;

        private AirwaySession(UUID patientId, long lastTickGameTime, long lastPublishedGameTime) {
            this.patientId = patientId;
            this.lastTickGameTime = lastTickGameTime;
            this.lastPublishedGameTime = lastPublishedGameTime;
        }
    }
}
