package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.CprResult;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CprService {
    private static final double MAX_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final long CPR_SECOND_TICKS = 20L;
    private static final long COMPRESSION_SOUND_INTERVAL_TICKS = 11L;
    private static final Map<UUID, CprSession> SESSION_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_PATIENT = new HashMap<>();

    private CprService() {
    }

    public static boolean start(ServerPlayer actor, int patientEntityId) {
        if (!(actor.serverLevel().getEntity(patientEntityId) instanceof ServerPlayer patient)
                || !canContinue(actor, patient)) {
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.cpr.invalid_target"
            ), true);
            return false;
        }
        UUID existingActor = ACTOR_BY_PATIENT.get(patient.getUUID());
        if (existingActor != null && !existingActor.equals(actor.getUUID())) {
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.cpr.patient_busy"
            ), true);
            return false;
        }

        TreatmentService.cancelInvolving(actor, TreatmentCancelReason.ACTION);
        AirwayService.cancelInvolving(actor);
        DefibrillationService.cancelInvolving(actor);
        stopActor(actor, actor.getUUID());
        long gameTime = actor.serverLevel().getGameTime();
        CprSession session = new CprSession(
                patient.getUUID(),
                gameTime,
                gameTime + COMPRESSION_SOUND_INTERVAL_TICKS
        );
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        MedicalActionSoundService.start(
                actor,
                patient,
                MedicalActionSoundChannel.CPR,
                MedicalActionSound.CPR
        );
        return true;
    }

    public static void stop(ServerPlayer actor, int patientEntityId) {
        CprSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || patient.getId() == patientEntityId) {
            stopActor(actor, actor.getUUID());
        }
    }

    public static void tick(ServerPlayer actor) {
        CprSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || !canContinue(actor, patient)) {
            stopActor(actor, actor.getUUID());
            return;
        }

        long gameTime = actor.serverLevel().getGameTime();
        if (gameTime >= session.nextCompressionSoundGameTime) {
            MedicalActionSoundService.start(
                    actor,
                    patient,
                    MedicalActionSoundChannel.CPR,
                    MedicalActionSound.CPR
            );
            session.nextCompressionSoundGameTime = gameTime + COMPRESSION_SOUND_INTERVAL_TICKS;
        }
        session.continuousTicks += Math.max(0L, gameTime - session.lastTickGameTime);
        session.lastTickGameTime = gameTime;
        int completedSeconds = (int) (session.continuousTicks / CPR_SECOND_TICKS);
        session.continuousTicks %= CPR_SECOND_TICKS;
        if (completedSeconds <= 0) {
            return;
        }

        BodyState state = BodyStateCapability.get(patient).orElse(null);
        if (state == null) {
            stopActor(actor, actor.getUUID());
            return;
        }
        CprResult result = null;
        for (int second = 0; second < completedSeconds; second++) {
            result = state.applyCprSecond(
                    actor.getRandom().nextDouble(),
                    actor.getRandom().nextDouble(),
                    gameTime
            );
            if (result.status() != CprResult.Status.CONTINUE) {
                break;
            }
        }
        if (result == null || result.status() == CprResult.Status.INVALID) {
            stopActor(actor, actor.getUUID());
            return;
        }
        if (result.succeeded()) {
            state.recordResuscitationContributor(actor.getUUID(), actor.getGameProfile().getName());
            String messageKey = result.status() == CprResult.Status.VENTRICULAR_FIBRILLATION
                    ? "message.superficialtrauma.cpr.success_vf"
                    : "message.superficialtrauma.cpr.success_stable";
            actor.displayClientMessage(Component.translatable(messageKey), true);
            stopActor(actor, actor.getUUID());
        }
        ModNetworking.syncBodyState(patient);
        InspectionService.syncPatient(patient);
    }

    public static void cancelInvolving(ServerPlayer player) {
        stopActor(player, player.getUUID());
        UUID actorId = ACTOR_BY_PATIENT.get(player.getUUID());
        if (actorId != null) {
            stopActor(player, actorId);
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
                || !InspectionService.isInspecting(actor, patient.getId())) {
            return false;
        }
        return BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false)
                && BodyStateCapability.get(patient)
                .map(BodyState::lifeState)
                .orElse(BodyLifeState.ACTIVE) == BodyLifeState.CARDIAC_ARREST;
    }

    private static void stopActor(ServerPlayer reference, UUID actorId) {
        CprSession removed = SESSION_BY_ACTOR.remove(actorId);
        if (removed != null) {
            ACTOR_BY_PATIENT.remove(removed.patientId, actorId);
            ServerPlayer actor = player(reference, actorId);
            ServerPlayer patient = player(reference, removed.patientId);
            if (actor != null) {
                MedicalActionSoundService.stop(
                        actor,
                        patient,
                        MedicalActionSoundChannel.CPR
                );
            }
        }
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static final class CprSession {
        private final UUID patientId;
        private long lastTickGameTime;
        private long nextCompressionSoundGameTime;
        private long continuousTicks;

        private CprSession(UUID patientId, long gameTime, long nextCompressionSoundGameTime) {
            this.patientId = patientId;
            this.lastTickGameTime = gameTime;
            this.nextCompressionSoundGameTime = nextCompressionSoundGameTime;
        }
    }
}
