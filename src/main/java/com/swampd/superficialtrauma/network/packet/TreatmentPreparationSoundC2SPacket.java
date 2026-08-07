package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationSoundService;
import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record TreatmentPreparationSoundC2SPacket(
        int patientEntityId,
        UUID woundId,
        TreatmentPreparationType type,
        boolean active
) {
    public static void encode(TreatmentPreparationSoundC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeEnum(packet.type);
        buffer.writeBoolean(packet.active);
    }

    public static TreatmentPreparationSoundC2SPacket decode(FriendlyByteBuf buffer) {
        return new TreatmentPreparationSoundC2SPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readEnum(TreatmentPreparationType.class),
                buffer.readBoolean()
        );
    }

    public static void handle(
            TreatmentPreparationSoundC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> TreatmentPreparationSoundService.set(
                    sender,
                    packet.patientEntityId,
                    packet.woundId,
                    packet.type,
                    packet.active
            ));
        }
        context.setPacketHandled(true);
    }
}
