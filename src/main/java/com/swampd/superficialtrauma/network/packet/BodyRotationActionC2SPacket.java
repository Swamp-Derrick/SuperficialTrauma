package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.drag.BodyRotationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BodyRotationActionC2SPacket(int targetEntityId, boolean holding) {
    public static void encode(BodyRotationActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.holding);
    }

    public static BodyRotationActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new BodyRotationActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            BodyRotationActionC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> BodyRotationService.setHolding(
                    sender,
                    packet.targetEntityId,
                    packet.holding
            ));
        }
        context.setPacketHandled(true);
    }
}
