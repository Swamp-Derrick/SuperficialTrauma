package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class InspectionService {
    private static final double MAX_OPEN_DISTANCE_SQUARED = 4.5D * 4.5D;
    private static final double MAX_CONTINUE_DISTANCE_SQUARED = 6.0D * 6.0D;
    private static final long PERIODIC_SYNC_TICKS = 10L;
    private static final Map<UUID, InspectionSession> SESSIONS_BY_INSPECTOR = new HashMap<>();

    private InspectionService() {
    }

    public static boolean open(ServerPlayer inspector, int targetEntityId) {
        TreatmentService.cancelInvolving(inspector, TreatmentCancelReason.ACTION);
        AirwayService.cancelInvolving(inspector);
        CprService.cancelInvolving(inspector);
        DefibrillationService.cancelInvolving(inspector);
        if (!(inspector.serverLevel().getEntity(targetEntityId) instanceof ServerPlayer patient)
                || !canInspect(inspector, patient, MAX_OPEN_DISTANCE_SQUARED, true)) {
            return false;
        }

        InspectionSession session = new InspectionSession(patient.getUUID());
        SESSIONS_BY_INSPECTOR.put(inspector.getUUID(), session);
        sendSnapshot(inspector, patient, session, true);
        return true;
    }

    public static void close(ServerPlayer inspector, int targetEntityId) {
        AirwayService.cancelInvolving(inspector);
        CprService.cancelInvolving(inspector);
        DefibrillationService.cancelInvolving(inspector);
        InspectionSession session = SESSIONS_BY_INSPECTOR.get(inspector.getUUID());
        if (session != null && session.patientEntityId == targetEntityId) {
            SESSIONS_BY_INSPECTOR.remove(inspector.getUUID());
            return;
        }
        if (session == null) {
            return;
        }
        ServerPlayer patient = playerById(inspector, session.patientId);
        if (patient != null && patient.getId() == targetEntityId) {
            SESSIONS_BY_INSPECTOR.remove(inspector.getUUID());
        }
    }

    public static void tick(ServerPlayer inspector) {
        InspectionSession session = SESSIONS_BY_INSPECTOR.get(inspector.getUUID());
        if (session == null) {
            return;
        }

        ServerPlayer patient = playerById(inspector, session.patientId);
        if (patient == null || !canInspect(inspector, patient, MAX_CONTINUE_DISTANCE_SQUARED, false)) {
            SESSIONS_BY_INSPECTOR.remove(inspector.getUUID());
            ModNetworking.closeInspection(inspector, session.patientEntityId);
            return;
        }

        long gameTime = inspector.serverLevel().getGameTime();
        long revision = BodyStateCapability.get(patient).map(bodyState -> bodyState.revision()).orElse(-1L);
        if (revision != session.lastRevision || gameTime - session.lastSyncGameTime >= PERIODIC_SYNC_TICKS) {
            sendSnapshot(inspector, patient, session, false);
        }
    }

    public static void syncPatient(ServerPlayer patient) {
        for (Map.Entry<UUID, InspectionSession> entry : SESSIONS_BY_INSPECTOR.entrySet()) {
            InspectionSession session = entry.getValue();
            if (!session.patientId.equals(patient.getUUID())) {
                continue;
            }
            ServerPlayer inspector = patient.getServer() == null
                    ? null
                    : patient.getServer().getPlayerList().getPlayer(entry.getKey());
            if (inspector != null && canInspect(inspector, patient, MAX_CONTINUE_DISTANCE_SQUARED, false)) {
                sendSnapshot(inspector, patient, session, false);
            }
        }
    }

    public static boolean isInspecting(ServerPlayer inspector, int patientEntityId) {
        InspectionSession session = SESSIONS_BY_INSPECTOR.get(inspector.getUUID());
        if (session == null) {
            return false;
        }
        if (session.patientEntityId == patientEntityId) {
            return true;
        }
        ServerPlayer patient = playerById(inspector, session.patientId);
        return patient != null && patient.getId() == patientEntityId;
    }

    public static void forgetPlayer(ServerPlayer player) {
        SESSIONS_BY_INSPECTOR.remove(player.getUUID());
        Iterator<Map.Entry<UUID, InspectionSession>> iterator = SESSIONS_BY_INSPECTOR.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, InspectionSession> entry = iterator.next();
            if (!entry.getValue().patientId.equals(player.getUUID())) {
                continue;
            }
            if (player.getServer() != null) {
                ServerPlayer inspector = player.getServer().getPlayerList().getPlayer(entry.getKey());
                if (inspector != null) {
                    ModNetworking.closeInspection(inspector, entry.getValue().patientEntityId);
                }
            }
            iterator.remove();
        }
    }

    public static void clearAll() {
        SESSIONS_BY_INSPECTOR.clear();
    }

    private static boolean canInspect(
            ServerPlayer inspector,
            ServerPlayer patient,
            double maximumDistanceSquared,
            boolean requireLineOfSight
    ) {
        if (inspector == patient
                || !inspector.isAlive()
                || inspector.isRemoved()
                || inspector.isSpectator()
                || !patient.isAlive()
                || patient.isRemoved()
                || inspector.serverLevel() != patient.serverLevel()
                || inspector.distanceToSqr(patient) > maximumDistanceSquared
                || (requireLineOfSight && !inspector.hasLineOfSight(patient))) {
            return false;
        }
        return BodyStateCapability.get(inspector).map(bodyState -> bodyState.canAct()).orElse(false);
    }

    private static ServerPlayer playerById(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static void sendSnapshot(
            ServerPlayer inspector,
            ServerPlayer patient,
            InspectionSession session,
            boolean openScreen
    ) {
        BodyStateCapability.get(patient).ifPresent(bodyState -> {
            session.patientEntityId = patient.getId();
            session.lastRevision = bodyState.revision();
            session.lastSyncGameTime = inspector.serverLevel().getGameTime();
            ModNetworking.sendInspectionSnapshot(inspector, patient, bodyState, openScreen);
        });
    }

    private static final class InspectionSession {
        private final UUID patientId;
        private int patientEntityId = -1;
        private long lastRevision = -1L;
        private long lastSyncGameTime = Long.MIN_VALUE;

        private InspectionSession(UUID patientId) {
            this.patientId = patientId;
        }
    }
}
