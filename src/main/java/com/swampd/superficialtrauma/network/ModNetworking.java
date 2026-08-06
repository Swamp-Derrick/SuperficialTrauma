package com.swampd.superficialtrauma.network;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.treatment.TreatmentCancelReason;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentSession;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
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
    private static final String PROTOCOL_VERSION = "14";
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

    public static void requestOwnBodyState() {
        CHANNEL.sendToServer(new RequestBodyStateC2SPacket());
    }

    public static void requestLootTarget(int targetEntityId) {
        CHANNEL.sendToServer(new RequestLootTargetC2SPacket(targetEntityId));
    }

    public static void requestInspection(int targetEntityId) {
        CHANNEL.sendToServer(new RequestInspectionC2SPacket(targetEntityId));
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

    public static void setAssistedBreathing(int patientEntityId, boolean active) {
        CHANNEL.sendToServer(new AirwayActionC2SPacket(patientEntityId, active));
    }

    public static void requestInfusion(int patientEntityId, InfusionType type) {
        CHANNEL.sendToServer(new StartInfusionC2SPacket(patientEntityId, type));
    }

    public static void setCpr(int patientEntityId, boolean active) {
        CHANNEL.sendToServer(new CprActionC2SPacket(patientEntityId, active));
    }

    public static void requestDefibrillation(int patientEntityId, DefibrillationEnergy energy) {
        CHANNEL.sendToServer(new StartDefibrillationC2SPacket(patientEntityId, energy.joules()));
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
