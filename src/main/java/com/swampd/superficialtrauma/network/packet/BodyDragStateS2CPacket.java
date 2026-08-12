package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBodyDragState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BodyDragStateS2CPacket(int targetEntityId, int draggerEntityId, boolean active) {
    public static void encode(BodyDragStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeVarInt(packet.draggerEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static BodyDragStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new BodyDragStateS2CPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(
            BodyDragStateS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBodyDragState.update(
                        packet.targetEntityId,
                        packet.draggerEntityId,
                        packet.active
                )
        ));
        context.setPacketHandled(true);
    }
}
