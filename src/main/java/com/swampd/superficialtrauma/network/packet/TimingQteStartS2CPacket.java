package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTimingQteState;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TimingQteStartS2CPacket(TimingQteSnapshot snapshot) {
    public static void encode(TimingQteStartS2CPacket packet, FriendlyByteBuf buffer) {
        TimingQteSnapshot snapshot = packet.snapshot;
        buffer.writeVarInt(snapshot.sessionId());
        buffer.writeLong(snapshot.cursorStartGameTime());
        buffer.writeVarInt(snapshot.sweepDurationTicks());
        buffer.writeFloat(snapshot.perfectStart());
        buffer.writeFloat(snapshot.normalStart());
        buffer.writeFloat(snapshot.successEnd());
    }

    public static TimingQteStartS2CPacket decode(FriendlyByteBuf buffer) {
        return new TimingQteStartS2CPacket(new TimingQteSnapshot(
                buffer.readVarInt(),
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        ));
    }

    public static void handle(TimingQteStartS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTimingQteState.start(packet.snapshot)
        ));
        context.setPacketHandled(true);
    }
}
