package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestBodyStateC2SPacket {
    public static void encode(RequestBodyStateC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestBodyStateC2SPacket decode(FriendlyByteBuf buffer) {
        return new RequestBodyStateC2SPacket();
    }

    public static void handle(
            RequestBodyStateC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> ModNetworking.handleBodyStateRequest(sender));
        }
        context.setPacketHandled(true);
    }
}
