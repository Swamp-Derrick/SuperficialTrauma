package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicationState;
import com.swampd.superficialtrauma.common.medication.MedicationCancelReason;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MedicationSessionS2CPacket(
        Status status,
        int patientEntityId,
        MedicationType type,
        long endsGameTime,
        MedicationCancelReason cancelReason
) {
    public static MedicationSessionS2CPacket started(
            int patientEntityId,
            MedicationType type,
            long endsGameTime
    ) {
        return new MedicationSessionS2CPacket(Status.STARTED, patientEntityId, type, endsGameTime, null);
    }

    public static MedicationSessionS2CPacket cancelled(
            MedicationType type,
            MedicationCancelReason reason
    ) {
        return new MedicationSessionS2CPacket(Status.CANCELLED, -1, type, -1L, reason);
    }

    public static MedicationSessionS2CPacket completed(int patientEntityId, MedicationType type) {
        return new MedicationSessionS2CPacket(Status.COMPLETED, patientEntityId, type, -1L, null);
    }

    public static void encode(MedicationSessionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.type.serializedName());
        buffer.writeLong(packet.endsGameTime);
        buffer.writeBoolean(packet.cancelReason != null);
        if (packet.cancelReason != null) {
            buffer.writeEnum(packet.cancelReason);
        }
    }

    public static MedicationSessionS2CPacket decode(FriendlyByteBuf buffer) {
        Status status = buffer.readEnum(Status.class);
        int patientEntityId = buffer.readVarInt();
        MedicationType type = MedicationType.fromNetworkName(buffer.readUtf(32)).orElse(null);
        long endsGameTime = buffer.readLong();
        MedicationCancelReason reason = buffer.readBoolean()
                ? buffer.readEnum(MedicationCancelReason.class)
                : null;
        return new MedicationSessionS2CPacket(status, patientEntityId, type, endsGameTime, reason);
    }

    public static void handle(
            MedicationSessionS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        if (packet.type == null) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (packet.status) {
                case STARTED -> ClientMedicationState.started(
                        packet.patientEntityId,
                        packet.type,
                        packet.endsGameTime
                );
                case CANCELLED -> ClientMedicationState.cancelled(packet.cancelReason);
                case COMPLETED -> ClientMedicationState.completed(packet.type);
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
