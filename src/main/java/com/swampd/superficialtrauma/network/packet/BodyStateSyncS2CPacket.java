package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBodyState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BodyStateSyncS2CPacket(CompoundTag bodyStateTag) {
    public BodyStateSyncS2CPacket {
        bodyStateTag = bodyStateTag.copy();
    }

    public static void encode(BodyStateSyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.bodyStateTag);
    }

    public static BodyStateSyncS2CPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new BodyStateSyncS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(
            BodyStateSyncS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBodyState.update(packet.bodyStateTag)
        ));
        context.setPacketHandled(true);
    }
}
