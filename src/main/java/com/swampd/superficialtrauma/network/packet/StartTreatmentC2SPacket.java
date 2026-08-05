package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record StartTreatmentC2SPacket(int patientEntityId, UUID woundId, TreatmentType treatmentType) {
    public static void encode(StartTreatmentC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeUtf(packet.treatmentType.serializedName());
    }

    public static StartTreatmentC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartTreatmentC2SPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                TreatmentType.fromSerializedName(buffer.readUtf(64))
        );
    }

    public static void handle(StartTreatmentC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> TreatmentService.start(
                    sender,
                    packet.patientEntityId,
                    packet.woundId,
                    packet.treatmentType
            ));
        }
        context.setPacketHandled(true);
    }
}
