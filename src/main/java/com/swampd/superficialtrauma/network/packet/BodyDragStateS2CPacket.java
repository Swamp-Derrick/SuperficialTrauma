package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBodyDragState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record BodyDragStateS2CPacket(int targetEntityId, int draggerEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<BodyDragStateS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "body_drag_state_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyDragStateS2CPacket> STREAM_CODEC = StreamCodec.ofMember(BodyDragStateS2CPacket::write, BodyDragStateS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BodyDragStateS2CPacket> type() { return TYPE; }

    public static void encode(BodyDragStateS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeVarInt(packet.draggerEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static BodyDragStateS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new BodyDragStateS2CPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(
            BodyDragStateS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientBodyDragState.update(
                        packet.targetEntityId,
                        packet.draggerEntityId,
                        packet.active
                )
        );
    }
}
