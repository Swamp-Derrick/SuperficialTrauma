package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTreatmentState;
import com.swampd.superficialtrauma.common.treatment.TreatmentCancelReason;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TreatmentSessionS2CPacket(
        Status status,
        int patientEntityId,
        UUID woundId,
        TreatmentProcedure procedure,
        TreatmentAction action,
        long endsGameTime,
        TreatmentCancelReason cancelReason
) implements CustomPacketPayload {
    public static final Type<TreatmentSessionS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "treatment_session_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TreatmentSessionS2CPacket> STREAM_CODEC = StreamCodec.ofMember(TreatmentSessionS2CPacket::write, TreatmentSessionS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<TreatmentSessionS2CPacket> type() { return TYPE; }

    public static TreatmentSessionS2CPacket started(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action,
            long endsGameTime
    ) {
        return new TreatmentSessionS2CPacket(
                Status.STARTED,
                patientEntityId,
                woundId,
                procedure,
                action,
                endsGameTime,
                null
        );
    }

    public static TreatmentSessionS2CPacket cancelled(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action,
            TreatmentCancelReason reason
    ) {
        return new TreatmentSessionS2CPacket(
                Status.CANCELLED,
                patientEntityId,
                woundId,
                procedure,
                action,
                -1L,
                reason
        );
    }

    public static TreatmentSessionS2CPacket completed(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action
    ) {
        return new TreatmentSessionS2CPacket(
                Status.COMPLETED,
                patientEntityId,
                woundId,
                procedure,
                action,
                -1L,
                null
        );
    }

    public static void encode(TreatmentSessionS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeUtf(packet.procedure.serializedName());
        buffer.writeUtf(packet.action.serializedName());
        buffer.writeLong(packet.endsGameTime);
        buffer.writeBoolean(packet.cancelReason != null);
        if (packet.cancelReason != null) {
            buffer.writeEnum(packet.cancelReason);
        }
    }

    public static TreatmentSessionS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        Status status = buffer.readEnum(Status.class);
        int patientEntityId = buffer.readVarInt();
        UUID woundId = buffer.readUUID();
        TreatmentProcedure procedure = TreatmentProcedure.fromSerializedName(buffer.readUtf(64));
        TreatmentAction action = TreatmentAction.fromSerializedName(buffer.readUtf(32));
        long endsGameTime = buffer.readLong();
        TreatmentCancelReason reason = buffer.readBoolean()
                ? buffer.readEnum(TreatmentCancelReason.class)
                : null;
        return new TreatmentSessionS2CPacket(
                status,
                patientEntityId,
                woundId,
                procedure,
                action,
                endsGameTime,
                reason
        );
    }

    public static void handle(TreatmentSessionS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (packet.status) {
                case STARTED -> ClientTreatmentState.started(
                        packet.patientEntityId,
                        packet.woundId,
                        packet.procedure,
                        packet.action,
                        packet.endsGameTime
                );
                case CANCELLED -> ClientTreatmentState.cancelled(packet.cancelReason);
                case COMPLETED -> ClientTreatmentState.completed(packet.procedure, packet.action);
            }
        });
    }

    public enum Status {
        STARTED,
        CANCELLED,
        COMPLETED
    }
}
