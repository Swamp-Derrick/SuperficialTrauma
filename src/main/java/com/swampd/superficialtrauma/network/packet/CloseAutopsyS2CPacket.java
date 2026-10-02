package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientAutopsyState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record CloseAutopsyS2CPacket(int corpseEntityId) implements CustomPacketPayload {
    public static final Type<CloseAutopsyS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "close_autopsy_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseAutopsyS2CPacket> STREAM_CODEC = StreamCodec.ofMember(CloseAutopsyS2CPacket::write, CloseAutopsyS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CloseAutopsyS2CPacket> type() { return TYPE; }

    public static void encode(CloseAutopsyS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
    }

    public static CloseAutopsyS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CloseAutopsyS2CPacket(buffer.readVarInt());
    }

    public static void handle(CloseAutopsyS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientAutopsyState.close(packet.corpseEntityId)
        );
    }
}
