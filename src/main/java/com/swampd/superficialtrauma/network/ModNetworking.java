package com.swampd.superficialtrauma.network;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.treatment.DefibrillationAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentCancelReason;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentSession;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationType;
import com.swampd.superficialtrauma.network.packet.BloodLossFeedbackS2CPacket;
import com.swampd.superficialtrauma.network.packet.BodyStateSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.DownedPoseSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.RequestBodyStateC2SPacket;
import com.swampd.superficialtrauma.network.packet.RequestLootTargetC2SPacket;
import com.swampd.superficialtrauma.network.packet.CloseInspectionC2SPacket;
import com.swampd.superficialtrauma.network.packet.CloseInspectionS2CPacket;
import com.swampd.superficialtrauma.network.packet.InspectionSnapshotS2CPacket;
import com.swampd.superficialtrauma.network.packet.RequestInspectionC2SPacket;
import com.swampd.superficialtrauma.network.packet.StartTreatmentC2SPacket;
import com.swampd.superficialtrauma.network.packet.TreatmentSessionS2CPacket;
import com.swampd.superficialtrauma.network.packet.AirwayActionC2SPacket;
import com.swampd.superficialtrauma.network.packet.StartInfusionC2SPacket;
import com.swampd.superficialtrauma.network.packet.CprActionC2SPacket;
import com.swampd.superficialtrauma.network.packet.StartDefibrillationC2SPacket;
import com.swampd.superficialtrauma.network.packet.DefibrillatorChargingSoundS2CPacket;
import com.swampd.superficialtrauma.network.packet.RequestAutopsyC2SPacket;
import com.swampd.superficialtrauma.network.packet.StartAutopsyActionC2SPacket;
import com.swampd.superficialtrauma.network.packet.CloseAutopsyC2SPacket;
import com.swampd.superficialtrauma.network.packet.AutopsyReportS2CPacket;
import com.swampd.superficialtrauma.network.packet.CloseAutopsyS2CPacket;
import com.swampd.superficialtrauma.network.packet.MedicalActionSoundS2CPacket;
import com.swampd.superficialtrauma.network.packet.TreatmentPreparationSoundC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "18";
    private static final long BODY_STATE_REQUEST_COOLDOWN_TICKS = 5L;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int nextPacketId;
    private static final ConcurrentMap<UUID, Long> LAST_BODY_STATE_REQUEST = new ConcurrentHashMap<>();

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                BodyStateSyncS2CPacket.class,
                BodyStateSyncS2CPacket::encode,
                BodyStateSyncS2CPacket::decode,
                BodyStateSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestBodyStateC2SPacket.class,
                RequestBodyStateC2SPacket::encode,
                RequestBodyStateC2SPacket::decode,
                RequestBodyStateC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                BloodLossFeedbackS2CPacket.class,
                BloodLossFeedbackS2CPacket::encode,
                BloodLossFeedbackS2CPacket::decode,
                BloodLossFeedbackS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                DownedPoseSyncS2CPacket.class,
                DownedPoseSyncS2CPacket::encode,
                DownedPoseSyncS2CPacket::decode,
                DownedPoseSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestLootTargetC2SPacket.class,
                RequestLootTargetC2SPacket::encode,
                RequestLootTargetC2SPacket::decode,
                RequestLootTargetC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestInspectionC2SPacket.class,
                RequestInspectionC2SPacket::encode,
                RequestInspectionC2SPacket::decode,
                RequestInspectionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CloseInspectionC2SPacket.class,
                CloseInspectionC2SPacket::encode,
                CloseInspectionC2SPacket::decode,
                CloseInspectionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                InspectionSnapshotS2CPacket.class,
                InspectionSnapshotS2CPacket::encode,
                InspectionSnapshotS2CPacket::decode,
                InspectionSnapshotS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CloseInspectionS2CPacket.class,
                CloseInspectionS2CPacket::encode,
                CloseInspectionS2CPacket::decode,
                CloseInspectionS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartTreatmentC2SPacket.class,
                StartTreatmentC2SPacket::encode,
                StartTreatmentC2SPacket::decode,
                StartTreatmentC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                TreatmentSessionS2CPacket.class,
                TreatmentSessionS2CPacket::encode,
                TreatmentSessionS2CPacket::decode,
                TreatmentSessionS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                TreatmentPreparationSoundC2SPacket.class,
                TreatmentPreparationSoundC2SPacket::encode,
                TreatmentPreparationSoundC2SPacket::decode,
                TreatmentPreparationSoundC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                MedicalActionSoundS2CPacket.class,
                MedicalActionSoundS2CPacket::encode,
                MedicalActionSoundS2CPacket::decode,
                MedicalActionSoundS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                AirwayActionC2SPacket.class,
                AirwayActionC2SPacket::encode,
                AirwayActionC2SPacket::decode,
                AirwayActionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartInfusionC2SPacket.class,
                StartInfusionC2SPacket::encode,
                StartInfusionC2SPacket::decode,
                StartInfusionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CprActionC2SPacket.class,
                CprActionC2SPacket::encode,
                CprActionC2SPacket::decode,
                CprActionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartDefibrillationC2SPacket.class,
                StartDefibrillationC2SPacket::encode,
                StartDefibrillationC2SPacket::decode,
                StartDefibrillationC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                DefibrillatorChargingSoundS2CPacket.class,
                DefibrillatorChargingSoundS2CPacket::encode,
                DefibrillatorChargingSoundS2CPacket::decode,
                DefibrillatorChargingSoundS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestAutopsyC2SPacket.class,
                RequestAutopsyC2SPacket::encode,
                RequestAutopsyC2SPacket::decode,
                RequestAutopsyC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartAutopsyActionC2SPacket.class,
                StartAutopsyActionC2SPacket::encode,
                StartAutopsyActionC2SPacket::decode,
                StartAutopsyActionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CloseAutopsyC2SPacket.class,
                CloseAutopsyC2SPacket::encode,
                CloseAutopsyC2SPacket::decode,
                CloseAutopsyC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                AutopsyReportS2CPacket.class,
                AutopsyReportS2CPacket::encode,
                AutopsyReportS2CPacket::decode,
                AutopsyReportS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CloseAutopsyS2CPacket.class,
                CloseAutopsyS2CPacket::encode,
                CloseAutopsyS2CPacket::decode,
                CloseAutopsyS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncBodyState(ServerPlayer player) {
        BodyStateCapability.get(player).ifPresent(bodyState -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BodyStateSyncS2CPacket(bodyState.serializeNBT())
        ));
    }

    public static void syncDownedPose(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                createDownedPosePacket(player)
        );
    }

    public static void syncDownedPoseTo(ServerPlayer subject, ServerPlayer receiver) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                createDownedPosePacket(subject)
        );
    }

    public static void clearDownedPoseFor(ServerPlayer receiver, int subjectEntityId) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                DownedPoseSyncS2CPacket.active(subjectEntityId)
        );
    }

    public static void syncDefibrillatorCharging(ServerPlayer actor, boolean charging) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> actor),
                new DefibrillatorChargingSoundS2CPacket(actor.getId(), charging)
        );
    }

    public static void syncDefibrillatorChargingTo(
            ServerPlayer actor,
            ServerPlayer receiver,
            boolean charging
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                new DefibrillatorChargingSoundS2CPacket(actor.getId(), charging)
        );
    }

    public static void requestOwnBodyState() {
        CHANNEL.sendToServer(new RequestBodyStateC2SPacket());
    }

    public static void requestLootTarget(int targetEntityId) {
        CHANNEL.sendToServer(new RequestLootTargetC2SPacket(targetEntityId));
    }

    public static void requestInspection(int targetEntityId) {
        CHANNEL.sendToServer(new RequestInspectionC2SPacket(targetEntityId));
    }

    public static void requestAutopsy(int corpseEntityId) {
        CHANNEL.sendToServer(new RequestAutopsyC2SPacket(corpseEntityId));
    }

    public static void requestAutopsyAction(int corpseEntityId, AutopsyAction action) {
        CHANNEL.sendToServer(new StartAutopsyActionC2SPacket(corpseEntityId, action));
    }

    public static void closeAutopsy(int corpseEntityId) {
        CHANNEL.sendToServer(new CloseAutopsyC2SPacket(corpseEntityId));
    }

    public static void closeInspection(int targetEntityId) {
        CHANNEL.sendToServer(new CloseInspectionC2SPacket(targetEntityId));
    }

    public static void requestTreatment(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action
    ) {
        CHANNEL.sendToServer(new StartTreatmentC2SPacket(patientEntityId, woundId, procedure, action));
    }

    public static void setTreatmentPreparationSound(
            int patientEntityId,
            UUID woundId,
            TreatmentPreparationType type,
            boolean active
    ) {
        CHANNEL.sendToServer(new TreatmentPreparationSoundC2SPacket(
                patientEntityId,
                woundId,
                type,
                active
        ));
    }

    public static void setAssistedBreathing(int patientEntityId, boolean active) {
        CHANNEL.sendToServer(new AirwayActionC2SPacket(patientEntityId, active));
    }

    public static void requestInfusion(int patientEntityId, InfusionType type) {
        CHANNEL.sendToServer(new StartInfusionC2SPacket(patientEntityId, type));
    }

    public static void setCpr(int patientEntityId, boolean active) {
        CHANNEL.sendToServer(new CprActionC2SPacket(patientEntityId, active));
    }

    public static void startDefibrillation(int patientEntityId, DefibrillationEnergy energy) {
        sendDefibrillationAction(patientEntityId, energy, DefibrillationAction.START);
    }

    public static void releaseDefibrillation(int patientEntityId, DefibrillationEnergy energy) {
        sendDefibrillationAction(patientEntityId, energy, DefibrillationAction.RELEASE);
    }

    public static void cancelDefibrillation(int patientEntityId, DefibrillationEnergy energy) {
        sendDefibrillationAction(patientEntityId, energy, DefibrillationAction.CANCEL);
    }

    private static void sendDefibrillationAction(
            int patientEntityId,
            DefibrillationEnergy energy,
            DefibrillationAction action
    ) {
        CHANNEL.sendToServer(new StartDefibrillationC2SPacket(
                patientEntityId,
                energy.joules(),
                action
        ));
    }

    public static void sendInspectionSnapshot(
            ServerPlayer inspector,
            ServerPlayer patient,
            BodyState bodyState,
            boolean openScreen
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> inspector),
                new InspectionSnapshotS2CPacket(
                        patient.getId(),
                        patient.getDisplayName(),
                        patient.getHealth(),
                        patient.getMaxHealth(),
                        bodyState.serializeNBT(),
                        openScreen
                )
        );
    }

    public static void closeInspection(ServerPlayer inspector, int patientEntityId) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> inspector),
                new CloseInspectionS2CPacket(patientEntityId)
        );
    }

    public static void sendAutopsyReport(ServerPlayer examiner, AutopsyReport report, boolean openScreen) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> examiner),
                new AutopsyReportS2CPacket(report.save(), openScreen)
        );
    }

    public static void closeAutopsy(ServerPlayer examiner, int corpseEntityId) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> examiner),
                new CloseAutopsyS2CPacket(corpseEntityId)
        );
    }

    public static void sendTreatmentStarted(ServerPlayer actor, int patientEntityId, TreatmentSession session) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> actor),
                TreatmentSessionS2CPacket.started(
                        patientEntityId,
                        session.woundId(),
                        session.procedure(),
                        session.action(),
                        session.endsGameTime()
                )
        );
    }

    public static void sendTreatmentCancelled(
            ServerPlayer actor,
            TreatmentSession session,
            TreatmentCancelReason reason
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> actor),
                TreatmentSessionS2CPacket.cancelled(
                        -1,
                        session.woundId(),
                        session.procedure(),
                        session.action(),
                        reason
                )
        );
    }

    public static void sendTreatmentCompleted(ServerPlayer actor, int patientEntityId, TreatmentSession session) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> actor),
                TreatmentSessionS2CPacket.completed(
                        patientEntityId,
                        session.woundId(),
                        session.procedure(),
                        session.action()
                )
        );
    }

    public static void sendMedicalActionSound(
            ServerPlayer actor,
            ServerPlayer receiver,
            MedicalActionSoundChannel channel,
            MedicalActionSound sound,
            boolean active
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                active
                        ? MedicalActionSoundS2CPacket.start(actor.getId(), channel, sound)
                        : MedicalActionSoundS2CPacket.stop(actor.getId(), channel)
        );
    }

    public static void stopMedicalActionSound(
            ServerPlayer actor,
            ServerPlayer receiver,
            MedicalActionSoundChannel channel
    ) {
        sendMedicalActionSound(
                actor,
                receiver,
                channel,
                MedicalActionSound.CLOTH_WRAPPING,
                false
        );
    }

    public static void playMedicalActionSoundOnce(
            ServerPlayer actor,
            ServerPlayer receiver,
            MedicalActionSound sound
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                MedicalActionSoundS2CPacket.playOnce(actor.getId(), sound)
        );
    }

    public static void sendBloodLossFeedback(ServerPlayer player, float amount) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BloodLossFeedbackS2CPacket(amount)
        );
    }

    public static void handleBodyStateRequest(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        Long previousRequest = LAST_BODY_STATE_REQUEST.get(player.getUUID());
        if (previousRequest != null
                && gameTime >= previousRequest
                && gameTime - previousRequest < BODY_STATE_REQUEST_COOLDOWN_TICKS) {
            return;
        }
        LAST_BODY_STATE_REQUEST.put(player.getUUID(), gameTime);
        syncBodyState(player);
    }

    public static void forgetPlayer(UUID playerId) {
        LAST_BODY_STATE_REQUEST.remove(playerId);
    }

    private static DownedPoseSyncS2CPacket createDownedPosePacket(ServerPlayer player) {
        return BodyStateCapability.get(player)
                .resolve()
                .flatMap(bodyState -> bodyState.downedPoseSnapshot())
                .map(snapshot -> DownedPoseSyncS2CPacket.downed(player.getId(), snapshot))
                .orElseGet(() -> DownedPoseSyncS2CPacket.active(player.getId()));
    }
}
