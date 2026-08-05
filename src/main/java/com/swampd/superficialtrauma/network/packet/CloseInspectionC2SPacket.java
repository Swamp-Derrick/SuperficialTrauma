package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.InspectionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseInspectionC2SPacket(int targetEntityId) {
    public static void encode(CloseInspectionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static CloseInspectionC2SPacket decode(FriendlyByteBuf buffer) {
        return new CloseInspectionC2SPacket(buffer.readVarInt());
    }

    public static void handle(CloseInspectionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> InspectionService.close(sender, packet.targetEntityId));
        }
        context.setPacketHandled(true);
    }
}
