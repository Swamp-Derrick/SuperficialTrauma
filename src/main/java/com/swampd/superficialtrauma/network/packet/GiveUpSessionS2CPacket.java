package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientGiveUpState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GiveUpSessionS2CPacket(Status status, long endsGameTime) {
    public static GiveUpSessionS2CPacket started(long endsGameTime) {
        return new GiveUpSessionS2CPacket(Status.STARTED, endsGameTime);
    }

    public static GiveUpSessionS2CPacket cancelled() {
        return new GiveUpSessionS2CPacket(Status.CANCELLED, -1L);
    }

    public static GiveUpSessionS2CPacket completed() {
        return new GiveUpSessionS2CPacket(Status.COMPLETED, -1L);
    }

    public static void encode(GiveUpSessionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeLong(packet.endsGameTime);
    }

    public static GiveUpSessionS2CPacket decode(FriendlyByteBuf buffer) {
        return new GiveUpSessionS2CPacket(buffer.readEnum(Status.class), buffer.readLong());
    }

    public static void handle(
            GiveUpSessionS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (packet.status == Status.STARTED) {
                ClientGiveUpState.started(packet.endsGameTime);
            } else {
                ClientGiveUpState.clear();
            }
        }));
        context.setPacketHandled(true);
    }

    public enum Status {
        STARTED,
        CANCELLED,
        COMPLETED
    }
}
