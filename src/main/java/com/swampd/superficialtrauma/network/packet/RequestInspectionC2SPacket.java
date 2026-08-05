package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.InspectionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestInspectionC2SPacket(int targetEntityId) {
    public static void encode(RequestInspectionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static RequestInspectionC2SPacket decode(FriendlyByteBuf buffer) {
        return new RequestInspectionC2SPacket(buffer.readVarInt());
    }

    public static void handle(RequestInspectionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> InspectionService.open(sender, packet.targetEntityId));
        }
        context.setPacketHandled(true);
    }
}
