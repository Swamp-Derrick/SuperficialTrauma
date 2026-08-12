package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.drag.BodyDragService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BodyDragActionC2SPacket(int targetEntityId, boolean holding) {
    public static void encode(BodyDragActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.holding);
    }

    public static BodyDragActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new BodyDragActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            BodyDragActionC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> BodyDragService.setHolding(
                    sender,
                    packet.targetEntityId,
                    packet.holding
            ));
        }
        context.setPacketHandled(true);
    }
}
