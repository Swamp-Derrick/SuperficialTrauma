package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientDefibrillatorSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DefibrillatorChargingSoundS2CPacket(int actorEntityId, boolean charging) {
    public static void encode(DefibrillatorChargingSoundS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actorEntityId);
        buffer.writeBoolean(packet.charging);
    }

    public static DefibrillatorChargingSoundS2CPacket decode(FriendlyByteBuf buffer) {
        return new DefibrillatorChargingSoundS2CPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            DefibrillatorChargingSoundS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDefibrillatorSounds.setCharging(
                        packet.actorEntityId,
                        packet.charging
                )
        ));
        context.setPacketHandled(true);
    }
}
