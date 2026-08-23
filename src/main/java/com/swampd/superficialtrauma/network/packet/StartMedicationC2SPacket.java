package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.medication.MedicationService;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StartMedicationC2SPacket(int patientEntityId, MedicationType type) {
    public static void encode(StartMedicationC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.type.serializedName());
    }

    public static StartMedicationC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartMedicationC2SPacket(
                buffer.readVarInt(),
                MedicationType.fromNetworkName(buffer.readUtf(32)).orElse(null)
        );
    }

    public static void handle(StartMedicationC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null && packet.type != null) {
            context.enqueueWork(() -> MedicationService.start(sender, packet.patientEntityId, packet.type));
        }
        context.setPacketHandled(true);
    }
}
