package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBodyState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record BodyStateSyncS2CPacket(CompoundTag bodyStateTag) implements CustomPacketPayload {
    public static final Type<BodyStateSyncS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "body_state_sync_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyStateSyncS2CPacket> STREAM_CODEC = StreamCodec.ofMember(BodyStateSyncS2CPacket::write, BodyStateSyncS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BodyStateSyncS2CPacket> type() { return TYPE; }

    public BodyStateSyncS2CPacket {
        bodyStateTag = bodyStateTag.copy();
    }

    public static void encode(BodyStateSyncS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeNbt(packet.bodyStateTag);
    }

    public static BodyStateSyncS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new BodyStateSyncS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(
            BodyStateSyncS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientBodyState.update(packet.bodyStateTag)
        );
    }
}
