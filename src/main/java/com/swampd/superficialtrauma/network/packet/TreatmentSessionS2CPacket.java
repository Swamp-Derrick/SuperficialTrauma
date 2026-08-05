package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTreatmentState;
import com.swampd.superficialtrauma.common.treatment.TreatmentCancelReason;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record TreatmentSessionS2CPacket(
        Status status,
        int patientEntityId,
        UUID woundId,
        TreatmentType treatmentType,
        TreatmentAction action,
        long endsGameTime,
        TreatmentCancelReason cancelReason
) {
    public static TreatmentSessionS2CPacket started(
            int patientEntityId,
            UUID woundId,
            TreatmentType type,
            TreatmentAction action,
            long endsGameTime
    ) {
        return new TreatmentSessionS2CPacket(
                Status.STARTED,
                patientEntityId,
                woundId,
                type,
                action,
                endsGameTime,
                null
        );
    }

    public static TreatmentSessionS2CPacket cancelled(
            int patientEntityId,
            UUID woundId,
            TreatmentType type,
            TreatmentAction action,
            TreatmentCancelReason reason
    ) {
        return new TreatmentSessionS2CPacket(
                Status.CANCELLED,
                patientEntityId,
                woundId,
                type,
                action,
                -1L,
                reason
        );
    }

    public static TreatmentSessionS2CPacket completed(
            int patientEntityId,
            UUID woundId,
            TreatmentType type,
            TreatmentAction action
    ) {
        return new TreatmentSessionS2CPacket(
                Status.COMPLETED,
                patientEntityId,
                woundId,
                type,
                action,
                -1L,
                null
        );
    }

    public static void encode(TreatmentSessionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeUtf(packet.treatmentType.serializedName());
        buffer.writeUtf(packet.action.serializedName());
        buffer.writeLong(packet.endsGameTime);
        buffer.writeBoolean(packet.cancelReason != null);
        if (packet.cancelReason != null) {
            buffer.writeEnum(packet.cancelReason);
        }
    }

    public static TreatmentSessionS2CPacket decode(FriendlyByteBuf buffer) {
        Status status = buffer.readEnum(Status.class);
        int patientEntityId = buffer.readVarInt();
        UUID woundId = buffer.readUUID();
        TreatmentType type = TreatmentType.fromSerializedName(buffer.readUtf(64));
        TreatmentAction action = TreatmentAction.fromSerializedName(buffer.readUtf(32));
        long endsGameTime = buffer.readLong();
        TreatmentCancelReason reason = buffer.readBoolean()
                ? buffer.readEnum(TreatmentCancelReason.class)
                : null;
        return new TreatmentSessionS2CPacket(status, patientEntityId, woundId, type, action, endsGameTime, reason);
    }

    public static void handle(TreatmentSessionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (packet.status) {
                case STARTED -> ClientTreatmentState.started(
                        packet.patientEntityId,
                        packet.woundId,
                        packet.treatmentType,
                        packet.action,
                        packet.endsGameTime
                );
                case CANCELLED -> ClientTreatmentState.cancelled(packet.cancelReason);
                case COMPLETED -> ClientTreatmentState.completed(packet.treatmentType, packet.action);
            }
        }));
        context.setPacketHandled(true);
    }

    public enum Status {
        STARTED,
        CANCELLED,
        COMPLETED
    }
}
