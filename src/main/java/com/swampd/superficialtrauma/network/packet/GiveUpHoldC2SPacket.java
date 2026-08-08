package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.GiveUpService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GiveUpHoldC2SPacket(boolean holding) {
    public static void encode(GiveUpHoldC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.holding);
    }

    public static GiveUpHoldC2SPacket decode(FriendlyByteBuf buffer) {
        return new GiveUpHoldC2SPacket(buffer.readBoolean());
    }

    public static void handle(GiveUpHoldC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> GiveUpService.setHolding(sender, packet.holding));
        }
        context.setPacketHandled(true);
    }
}
