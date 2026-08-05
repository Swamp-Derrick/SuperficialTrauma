package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.AirwayService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AirwayActionC2SPacket(int patientEntityId, boolean active) {
    public static void encode(AirwayActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static AirwayActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new AirwayActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(AirwayActionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (packet.active) {
                    AirwayService.start(sender, packet.patientEntityId);
                } else {
                    AirwayService.stop(sender, packet.patientEntityId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
