package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.CprService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CprActionC2SPacket(int patientEntityId, boolean active) {
    public static void encode(CprActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static CprActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new CprActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(CprActionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (packet.active) {
                    CprService.start(sender, packet.patientEntityId);
                } else {
                    CprService.stop(sender, packet.patientEntityId);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
