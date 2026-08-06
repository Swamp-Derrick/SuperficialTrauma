package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.treatment.DefibrillationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StartDefibrillationC2SPacket(int patientEntityId, int joules) {
    public static void encode(StartDefibrillationC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeVarInt(packet.joules);
    }

    public static StartDefibrillationC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartDefibrillationC2SPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            StartDefibrillationC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> DefibrillationService.start(
                    sender,
                    packet.patientEntityId,
                    DefibrillationEnergy.fromJoules(packet.joules)
            ));
        }
        context.setPacketHandled(true);
    }
}
