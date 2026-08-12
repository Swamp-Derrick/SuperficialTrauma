package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTimingQteState;
import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TimingQteResultS2CPacket(int sessionId, TimingQteResult result) {
    public static void encode(TimingQteResultS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeUtf(packet.result.serializedName());
    }

    public static TimingQteResultS2CPacket decode(FriendlyByteBuf buffer) {
        return new TimingQteResultS2CPacket(
                buffer.readVarInt(),
                TimingQteResult.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(TimingQteResultS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTimingQteState.resolve(packet.sessionId, packet.result)
        ));
        context.setPacketHandled(true);
    }
}
