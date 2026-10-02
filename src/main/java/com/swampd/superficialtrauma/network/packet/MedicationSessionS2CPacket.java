package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicationState;
import com.swampd.superficialtrauma.common.medication.MedicationCancelReason;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record MedicationSessionS2CPacket(
        Status status,
        int patientEntityId,
        MedicationType selectedType,
        long endsGameTime,
        MedicationCancelReason cancelReason
) implements CustomPacketPayload {
    public static final Type<MedicationSessionS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "medication_session_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicationSessionS2CPacket> STREAM_CODEC = StreamCodec.ofMember(MedicationSessionS2CPacket::write, MedicationSessionS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MedicationSessionS2CPacket> type() { return TYPE; }

    public static MedicationSessionS2CPacket started(
            int patientEntityId,
            MedicationType selectedType,
            long endsGameTime
    ) {
        return new MedicationSessionS2CPacket(Status.STARTED, patientEntityId, selectedType, endsGameTime, null);
    }

    public static MedicationSessionS2CPacket cancelled(
            MedicationType selectedType,
            MedicationCancelReason reason
    ) {
        return new MedicationSessionS2CPacket(Status.CANCELLED, -1, selectedType, -1L, reason);
    }

    public static MedicationSessionS2CPacket completed(int patientEntityId, MedicationType selectedType) {
        return new MedicationSessionS2CPacket(Status.COMPLETED, patientEntityId, selectedType, -1L, null);
    }

    public static void encode(MedicationSessionS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.selectedType.serializedName());
        buffer.writeLong(packet.endsGameTime);
        buffer.writeBoolean(packet.cancelReason != null);
        if (packet.cancelReason != null) {
            buffer.writeEnum(packet.cancelReason);
        }
    }

    public static MedicationSessionS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        Status status = buffer.readEnum(Status.class);
        int patientEntityId = buffer.readVarInt();
        MedicationType selectedType = MedicationType.fromNetworkName(buffer.readUtf(32)).orElse(null);
        long endsGameTime = buffer.readLong();
        MedicationCancelReason reason = buffer.readBoolean()
                ? buffer.readEnum(MedicationCancelReason.class)
                : null;
        return new MedicationSessionS2CPacket(status, patientEntityId, selectedType, endsGameTime, reason);
    }

    public static void handle(
            MedicationSessionS2CPacket packet,
            IPayloadContext context
    ) {
        if (packet.selectedType == null) {
            return;
        }
        context.enqueueWork(() -> {
            switch (packet.status) {
                case STARTED -> ClientMedicationState.started(
                        packet.patientEntityId,
                        packet.selectedType,
                        packet.endsGameTime
                );
                case CANCELLED -> ClientMedicationState.cancelled(packet.cancelReason);
                case COMPLETED -> ClientMedicationState.completed(packet.selectedType);
            }
        });
    }

    public enum Status {
        STARTED,
        CANCELLED,
        COMPLETED
    }
}
