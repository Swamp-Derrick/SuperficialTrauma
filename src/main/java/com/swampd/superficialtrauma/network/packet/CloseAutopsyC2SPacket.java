package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseAutopsyC2SPacket(int corpseEntityId) {
    public static void encode(CloseAutopsyC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
    }

    public static CloseAutopsyC2SPacket decode(FriendlyByteBuf buffer) {
        return new CloseAutopsyC2SPacket(buffer.readVarInt());
    }

    public static void handle(CloseAutopsyC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> AutopsyService.close(sender, packet.corpseEntityId));
        }
        context.setPacketHandled(true);
    }
}
