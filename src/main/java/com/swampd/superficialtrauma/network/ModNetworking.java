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
import com.swampd.superficialtrauma.common.medication.MedicationCancelReason;
import com.swampd.superficialtrauma.common.medication.MedicationSession;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationType;
import com.swampd.superficialtrauma.network.packet.BloodLossFeedbackS2CPacket;
import com.swampd.superficialtrauma.network.packet.BodyStateSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.DownedPoseSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.RequestBodyStateC2SPacket;
import com.swampd.superficialtrauma.network.packet.RequestLootTargetC2SPacket;
import com.swampd.superficialtrauma.network.packet.CloseInspectionC2SPacket;
import com.swampd.superficialtrauma.network.packet.CancelSkinGraftC2SPacket;
import com.swampd.superficialtrauma.network.packet.CloseInspectionS2CPacket;
import com.swampd.superficialtrauma.network.packet.InspectionSnapshotS2CPacket;
import com.swampd.superficialtrauma.network.packet.MedicalInspectionNoticeS2CPacket;
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
import com.swampd.superficialtrauma.network.packet.StartMedicationC2SPacket;
import com.swampd.superficialtrauma.network.packet.MedicationPreparationC2SPacket;
import com.swampd.superficialtrauma.network.packet.MedicationSessionS2CPacket;
import com.swampd.superficialtrauma.network.packet.GiveUpHoldC2SPacket;
import com.swampd.superficialtrauma.network.packet.GiveUpSessionS2CPacket;
import com.swampd.superficialtrauma.network.packet.TimingQteResultS2CPacket;
import com.swampd.superficialtrauma.network.packet.TimingQteStartS2CPacket;
import com.swampd.superficialtrauma.network.packet.TimingQteSubmitC2SPacket;
import com.swampd.superficialtrauma.network.packet.BodyDragActionC2SPacket;
import com.swampd.superficialtrauma.network.packet.BodyDragStateS2CPacket;
import com.swampd.superficialtrauma.network.packet.BodyRotationActionC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.21.1-1";
    private static final long BODY_STATE_REQUEST_COOLDOWN_TICKS = 5L;
    private static final ConcurrentMap<UUID, Long> LAST_BODY_STATE_REQUEST = new ConcurrentHashMap<>();

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CancelSkinGraftC2SPacket.TYPE, CancelSkinGraftC2SPacket.STREAM_CODEC, CancelSkinGraftC2SPacket::handle);
        registrar.playToClient(BodyStateSyncS2CPacket.TYPE, BodyStateSyncS2CPacket.STREAM_CODEC, BodyStateSyncS2CPacket::handle);
        registrar.playToServer(RequestBodyStateC2SPacket.TYPE, RequestBodyStateC2SPacket.STREAM_CODEC, RequestBodyStateC2SPacket::handle);
        registrar.playToClient(BloodLossFeedbackS2CPacket.TYPE, BloodLossFeedbackS2CPacket.STREAM_CODEC, BloodLossFeedbackS2CPacket::handle);
        registrar.playToClient(DownedPoseSyncS2CPacket.TYPE, DownedPoseSyncS2CPacket.STREAM_CODEC, DownedPoseSyncS2CPacket::handle);
        registrar.playToServer(RequestLootTargetC2SPacket.TYPE, RequestLootTargetC2SPacket.STREAM_CODEC, RequestLootTargetC2SPacket::handle);
        registrar.playToServer(RequestInspectionC2SPacket.TYPE, RequestInspectionC2SPacket.STREAM_CODEC, RequestInspectionC2SPacket::handle);
        registrar.playToServer(CloseInspectionC2SPacket.TYPE, CloseInspectionC2SPacket.STREAM_CODEC, CloseInspectionC2SPacket::handle);
        registrar.playToClient(InspectionSnapshotS2CPacket.TYPE, InspectionSnapshotS2CPacket.STREAM_CODEC, InspectionSnapshotS2CPacket::handle);
        registrar.playToClient(CloseInspectionS2CPacket.TYPE, CloseInspectionS2CPacket.STREAM_CODEC, CloseInspectionS2CPacket::handle);
        registrar.playToClient(MedicalInspectionNoticeS2CPacket.TYPE, MedicalInspectionNoticeS2CPacket.STREAM_CODEC, MedicalInspectionNoticeS2CPacket::handle);
        registrar.playToServer(StartTreatmentC2SPacket.TYPE, StartTreatmentC2SPacket.STREAM_CODEC, StartTreatmentC2SPacket::handle);
        registrar.playToClient(TreatmentSessionS2CPacket.TYPE, TreatmentSessionS2CPacket.STREAM_CODEC, TreatmentSessionS2CPacket::handle);
        registrar.playToServer(TreatmentPreparationSoundC2SPacket.TYPE, TreatmentPreparationSoundC2SPacket.STREAM_CODEC, TreatmentPreparationSoundC2SPacket::handle);
        registrar.playToClient(MedicalActionSoundS2CPacket.TYPE, MedicalActionSoundS2CPacket.STREAM_CODEC, MedicalActionSoundS2CPacket::handle);
        registrar.playToServer(StartMedicationC2SPacket.TYPE, StartMedicationC2SPacket.STREAM_CODEC, StartMedicationC2SPacket::handle);
        registrar.playToServer(MedicationPreparationC2SPacket.TYPE, MedicationPreparationC2SPacket.STREAM_CODEC, MedicationPreparationC2SPacket::handle);
        registrar.playToClient(MedicationSessionS2CPacket.TYPE, MedicationSessionS2CPacket.STREAM_CODEC, MedicationSessionS2CPacket::handle);
        registrar.playToServer(GiveUpHoldC2SPacket.TYPE, GiveUpHoldC2SPacket.STREAM_CODEC, GiveUpHoldC2SPacket::handle);
        registrar.playToClient(GiveUpSessionS2CPacket.TYPE, GiveUpSessionS2CPacket.STREAM_CODEC, GiveUpSessionS2CPacket::handle);
        registrar.playToServer(AirwayActionC2SPacket.TYPE, AirwayActionC2SPacket.STREAM_CODEC, AirwayActionC2SPacket::handle);
        registrar.playToServer(StartInfusionC2SPacket.TYPE, StartInfusionC2SPacket.STREAM_CODEC, StartInfusionC2SPacket::handle);
        registrar.playToServer(CprActionC2SPacket.TYPE, CprActionC2SPacket.STREAM_CODEC, CprActionC2SPacket::handle);
        registrar.playToServer(StartDefibrillationC2SPacket.TYPE, StartDefibrillationC2SPacket.STREAM_CODEC, StartDefibrillationC2SPacket::handle);
        registrar.playToClient(DefibrillatorChargingSoundS2CPacket.TYPE, DefibrillatorChargingSoundS2CPacket.STREAM_CODEC, DefibrillatorChargingSoundS2CPacket::handle);
        registrar.playToServer(RequestAutopsyC2SPacket.TYPE, RequestAutopsyC2SPacket.STREAM_CODEC, RequestAutopsyC2SPacket::handle);
        registrar.playToServer(StartAutopsyActionC2SPacket.TYPE, StartAutopsyActionC2SPacket.STREAM_CODEC, StartAutopsyActionC2SPacket::handle);
        registrar.playToServer(CloseAutopsyC2SPacket.TYPE, CloseAutopsyC2SPacket.STREAM_CODEC, CloseAutopsyC2SPacket::handle);
        registrar.playToClient(AutopsyReportS2CPacket.TYPE, AutopsyReportS2CPacket.STREAM_CODEC, AutopsyReportS2CPacket::handle);
        registrar.playToClient(CloseAutopsyS2CPacket.TYPE, CloseAutopsyS2CPacket.STREAM_CODEC, CloseAutopsyS2CPacket::handle);
        registrar.playToClient(TimingQteStartS2CPacket.TYPE, TimingQteStartS2CPacket.STREAM_CODEC, TimingQteStartS2CPacket::handle);
        registrar.playToServer(TimingQteSubmitC2SPacket.TYPE, TimingQteSubmitC2SPacket.STREAM_CODEC, TimingQteSubmitC2SPacket::handle);
        registrar.playToClient(TimingQteResultS2CPacket.TYPE, TimingQteResultS2CPacket.STREAM_CODEC, TimingQteResultS2CPacket::handle);
        registrar.playToServer(BodyDragActionC2SPacket.TYPE, BodyDragActionC2SPacket.STREAM_CODEC, BodyDragActionC2SPacket::handle);
        registrar.playToClient(BodyDragStateS2CPacket.TYPE, BodyDragStateS2CPacket.STREAM_CODEC, BodyDragStateS2CPacket::handle);
        registrar.playToServer(BodyRotationActionC2SPacket.TYPE, BodyRotationActionC2SPacket.STREAM_CODEC, BodyRotationActionC2SPacket::handle);
    }

    public static void syncBodyState(ServerPlayer player) {
        com.swampd.superficialtrauma.common.voice.ServerVoicechatState.refresh(player);
        BodyStateCapability.get(player).ifPresent(bodyState -> PacketDistributor.sendToPlayer(player,
                new BodyStateSyncS2CPacket(bodyState.serializeNBT())
        ));
    }

    public static void syncDownedPose(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                createDownedPosePacket(player)
        );
    }

    public static void syncDownedPoseTo(ServerPlayer subject, ServerPlayer receiver) {
        PacketDistributor.sendToPlayer(receiver,
                createDownedPosePacket(subject)
        );
    }

    public static void clearDownedPoseFor(ServerPlayer receiver, int subjectEntityId) {
        PacketDistributor.sendToPlayer(receiver,
                DownedPoseSyncS2CPacket.active(subjectEntityId)
        );
    }

    public static void syncDefibrillatorCharging(ServerPlayer actor, boolean charging) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(actor,
                new DefibrillatorChargingSoundS2CPacket(actor.getId(), charging)
        );
    }

    public static void syncDefibrillatorChargingTo(
            ServerPlayer actor,
            ServerPlayer receiver,
            boolean charging
    ) {
        PacketDistributor.sendToPlayer(receiver,
                new DefibrillatorChargingSoundS2CPacket(actor.getId(), charging)
        );
    }

    public static void requestOwnBodyState() {
        PacketDistributor.sendToServer(new RequestBodyStateC2SPacket());
    }

    public static void requestLootTarget(int targetEntityId) {
        PacketDistributor.sendToServer(new RequestLootTargetC2SPacket(targetEntityId));
    }

    public static void requestInspection(int targetEntityId) {
        PacketDistributor.sendToServer(new RequestInspectionC2SPacket(targetEntityId));
    }

    public static void requestAutopsy(int corpseEntityId) {
        PacketDistributor.sendToServer(new RequestAutopsyC2SPacket(corpseEntityId));
    }

    public static void requestAutopsyAction(int corpseEntityId, AutopsyAction action) {
        PacketDistributor.sendToServer(new StartAutopsyActionC2SPacket(corpseEntityId, action));
    }

    public static void closeAutopsy(int corpseEntityId) {
        PacketDistributor.sendToServer(new CloseAutopsyC2SPacket(corpseEntityId));
    }

    public static void submitTimingQte(int sessionId, float elapsedTicks, boolean pressed) {
        PacketDistributor.sendToServer(new TimingQteSubmitC2SPacket(sessionId, elapsedTicks, pressed));
    }

    public static void closeInspection(int targetEntityId) {
        PacketDistributor.sendToServer(new CloseInspectionC2SPacket(targetEntityId));
    }

    public static void cancelSkinGraft() {
        PacketDistributor.sendToServer(new CancelSkinGraftC2SPacket());
    }

    public static void requestTreatment(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action
    ) {
        PacketDistributor.sendToServer(new StartTreatmentC2SPacket(patientEntityId, woundId, procedure, action));
    }

    public static void setTreatmentPreparationSound(
            int patientEntityId,
            UUID woundId,
            TreatmentPreparationType type,
            boolean active
    ) {
        PacketDistributor.sendToServer(new TreatmentPreparationSoundC2SPacket(
                patientEntityId,
                woundId,
                type,
                active
        ));
    }

    public static void requestMedication(int patientEntityId, MedicationType type) {
        PacketDistributor.sendToServer(new StartMedicationC2SPacket(patientEntityId, type));
    }

    public static void setMedicationPreparation(int patientEntityId, boolean active) {
        PacketDistributor.sendToServer(new MedicationPreparationC2SPacket(patientEntityId, active));
    }

    public static void setGiveUpHolding(boolean holding) {
        PacketDistributor.sendToServer(new GiveUpHoldC2SPacket(holding));
    }

    public static void setBodyDragHolding(int targetEntityId, boolean holding) {
        PacketDistributor.sendToServer(new BodyDragActionC2SPacket(targetEntityId, holding));
    }

    public static void setBodyRotationHolding(int targetEntityId, boolean holding) {
        PacketDistributor.sendToServer(new BodyRotationActionC2SPacket(targetEntityId, holding));
    }

    public static void setAssistedBreathing(int patientEntityId, boolean active) {
        PacketDistributor.sendToServer(new AirwayActionC2SPacket(patientEntityId, active));
    }

    public static void requestInfusion(int patientEntityId, InfusionType type) {
        PacketDistributor.sendToServer(new StartInfusionC2SPacket(patientEntityId, type));
    }

    public static void setCpr(int patientEntityId, boolean active) {
        PacketDistributor.sendToServer(new CprActionC2SPacket(patientEntityId, active));
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
        PacketDistributor.sendToServer(new StartDefibrillationC2SPacket(
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
        PacketDistributor.sendToPlayer(inspector,
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
        PacketDistributor.sendToPlayer(inspector,
                new CloseInspectionS2CPacket(patientEntityId)
        );
    }

    public static void sendMedicalInspectionNotice(
            ServerPlayer patient,
            ServerPlayer inspector,
            net.minecraft.network.chat.Component actionItemName
    ) {
        PacketDistributor.sendToPlayer(patient,
                new MedicalInspectionNoticeS2CPacket(
                        inspector.getGameProfile().getName(),
                        actionItemName
                )
        );
    }

    public static void sendAutopsyReport(ServerPlayer examiner, AutopsyReport report, boolean openScreen) {
        PacketDistributor.sendToPlayer(examiner,
                new AutopsyReportS2CPacket(report.save(), openScreen)
        );
    }

    public static void closeAutopsy(ServerPlayer examiner, int corpseEntityId) {
        PacketDistributor.sendToPlayer(examiner,
                new CloseAutopsyS2CPacket(corpseEntityId)
        );
    }

    public static void sendTimingQteStarted(ServerPlayer player, TimingQteSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player,
                new TimingQteStartS2CPacket(snapshot)
        );
    }

    public static void sendTimingQteResolved(
            ServerPlayer player,
            int sessionId,
            TimingQteResult result
    ) {
        PacketDistributor.sendToPlayer(player,
                new TimingQteResultS2CPacket(sessionId, result)
        );
    }

    public static void sendTreatmentStarted(ServerPlayer actor, int patientEntityId, TreatmentSession session) {
        PacketDistributor.sendToPlayer(actor,
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
        PacketDistributor.sendToPlayer(actor,
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
        PacketDistributor.sendToPlayer(actor,
                TreatmentSessionS2CPacket.completed(
                        patientEntityId,
                        session.woundId(),
                        session.procedure(),
                        session.action()
                )
        );
    }

    public static void sendMedicationStarted(
            ServerPlayer actor,
            int patientEntityId,
            MedicationSession session
    ) {
        PacketDistributor.sendToPlayer(actor,
                MedicationSessionS2CPacket.started(
                        patientEntityId,
                        session.type(),
                        session.endsGameTime()
                )
        );
    }

    public static void sendMedicationCancelled(
            ServerPlayer actor,
            MedicationSession session,
            MedicationCancelReason reason
    ) {
        PacketDistributor.sendToPlayer(actor,
                MedicationSessionS2CPacket.cancelled(session.type(), reason)
        );
    }

    public static void sendMedicationCompleted(
            ServerPlayer actor,
            int patientEntityId,
            MedicationSession session
    ) {
        PacketDistributor.sendToPlayer(actor,
                MedicationSessionS2CPacket.completed(patientEntityId, session.type())
        );
    }

    public static void sendGiveUpStarted(ServerPlayer player, long endsGameTime) {
        PacketDistributor.sendToPlayer(player,
                GiveUpSessionS2CPacket.started(endsGameTime)
        );
    }

    public static void sendGiveUpCancelled(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                GiveUpSessionS2CPacket.cancelled()
        );
    }

    public static void sendGiveUpCompleted(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                GiveUpSessionS2CPacket.completed()
        );
    }

    public static void sendMedicalActionSound(
            ServerPlayer actor,
            ServerPlayer receiver,
            MedicalActionSoundChannel channel,
            MedicalActionSound sound,
            boolean active
    ) {
        PacketDistributor.sendToPlayer(receiver,
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
        PacketDistributor.sendToPlayer(receiver,
                MedicalActionSoundS2CPacket.playOnce(actor.getId(), sound)
        );
    }

    public static void sendBloodLossFeedback(ServerPlayer player, float amount) {
        PacketDistributor.sendToPlayer(player,
                new BloodLossFeedbackS2CPacket(amount)
        );
    }

    public static void syncBodyDragState(Entity target, ServerPlayer dragger, boolean active) {
        if (target == null || dragger == null) {
            return;
        }
        syncBodyDragState(target.getId(), dragger.getId(), active);
    }

    public static void syncBodyDragState(
            int targetEntityId,
            int draggerEntityId,
            boolean active
    ) {
        PacketDistributor.sendToAllPlayers(
                new BodyDragStateS2CPacket(targetEntityId, draggerEntityId, active)
        );
    }

    public static void syncBodyDragStateTo(
            Entity target,
            ServerPlayer dragger,
            ServerPlayer receiver,
            boolean active
    ) {
        PacketDistributor.sendToPlayer(receiver,
                new BodyDragStateS2CPacket(target.getId(), dragger.getId(), active)
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
                .flatMap(bodyState -> bodyState.downedPoseSnapshot())
                .map(snapshot -> DownedPoseSyncS2CPacket.downed(player.getId(), snapshot))
                .orElseGet(() -> DownedPoseSyncS2CPacket.active(player.getId()));
    }
}
