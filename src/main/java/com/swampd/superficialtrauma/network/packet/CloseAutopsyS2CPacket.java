package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientAutopsyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseAutopsyS2CPacket(int corpseEntityId) {
    public static void encode(CloseAutopsyS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
    }

    public static CloseAutopsyS2CPacket decode(FriendlyByteBuf buffer) {
        return new CloseAutopsyS2CPacket(buffer.readVarInt());
    }

    public static void handle(CloseAutopsyS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAutopsyState.close(packet.corpseEntityId)
        ));
        context.setPacketHandled(true);
    }
}
