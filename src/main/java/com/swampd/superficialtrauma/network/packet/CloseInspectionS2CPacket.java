package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientInspectionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseInspectionS2CPacket(int targetEntityId) {
    public static void encode(CloseInspectionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static CloseInspectionS2CPacket decode(FriendlyByteBuf buffer) {
        return new CloseInspectionS2CPacket(buffer.readVarInt());
    }

    public static void handle(CloseInspectionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientInspectionState.close(packet.targetEntityId)
        ));
        context.setPacketHandled(true);
    }
}
