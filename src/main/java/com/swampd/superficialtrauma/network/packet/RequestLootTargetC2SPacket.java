package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.loot.LootingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestLootTargetC2SPacket(int targetEntityId) {
    public static void encode(RequestLootTargetC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static RequestLootTargetC2SPacket decode(FriendlyByteBuf buffer) {
        return new RequestLootTargetC2SPacket(buffer.readVarInt());
    }

    public static void handle(
            RequestLootTargetC2SPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> LootingService.tryOpen(sender, packet.targetEntityId));
        }
        context.setPacketHandled(true);
    }
}
