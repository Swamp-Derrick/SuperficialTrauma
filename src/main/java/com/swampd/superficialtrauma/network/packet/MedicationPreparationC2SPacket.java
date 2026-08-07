package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.medication.MedicationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MedicationPreparationC2SPacket(int patientEntityId, boolean active) {
    public static void encode(MedicationPreparationC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static MedicationPreparationC2SPacket decode(FriendlyByteBuf buffer) {
        return new MedicationPreparationC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            MedicationPreparationC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> MedicationService.setInjectionPreparation(
                    sender,
                    packet.patientEntityId,
                    packet.active
            ));
        }
        context.setPacketHandled(true);
    }
}
