package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.qte.TimingQteService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TimingQteSubmitC2SPacket(int sessionId, float elapsedTicks, boolean pressed) {
    public static void encode(TimingQteSubmitC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeFloat(packet.elapsedTicks);
        buffer.writeBoolean(packet.pressed);
    }

    public static TimingQteSubmitC2SPacket decode(FriendlyByteBuf buffer) {
        return new TimingQteSubmitC2SPacket(
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(TimingQteSubmitC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> TimingQteService.submit(
                    sender,
                    packet.sessionId,
                    packet.elapsedTicks,
                    packet.pressed
            ));
        }
        context.setPacketHandled(true);
    }
}
