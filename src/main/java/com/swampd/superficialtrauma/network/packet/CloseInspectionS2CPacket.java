package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientInspectionState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record CloseInspectionS2CPacket(int targetEntityId) implements CustomPacketPayload {
    public static final Type<CloseInspectionS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "close_inspection_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseInspectionS2CPacket> STREAM_CODEC = StreamCodec.ofMember(CloseInspectionS2CPacket::write, CloseInspectionS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CloseInspectionS2CPacket> type() { return TYPE; }

    public static void encode(CloseInspectionS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static CloseInspectionS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CloseInspectionS2CPacket(buffer.readVarInt());
    }

    public static void handle(CloseInspectionS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientInspectionState.close(packet.targetEntityId)
        );
    }
}
