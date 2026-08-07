package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StartAutopsyActionC2SPacket(int corpseEntityId, AutopsyAction action) {
    public StartAutopsyActionC2SPacket {
        action = action == null ? AutopsyAction.NONE : action;
    }

    public static void encode(StartAutopsyActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
        buffer.writeUtf(packet.action.serializedName());
    }

    public static StartAutopsyActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartAutopsyActionC2SPacket(
                buffer.readVarInt(),
                AutopsyAction.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartAutopsyActionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> AutopsyService.start(sender, packet.corpseEntityId, packet.action));
        }
        context.setPacketHandled(true);
    }
}
