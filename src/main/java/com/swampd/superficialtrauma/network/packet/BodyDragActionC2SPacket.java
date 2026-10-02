package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.drag.BodyDragService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record BodyDragActionC2SPacket(int targetEntityId, boolean holding) implements CustomPacketPayload {
    public static final Type<BodyDragActionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "body_drag_action_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyDragActionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(BodyDragActionC2SPacket::write, BodyDragActionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BodyDragActionC2SPacket> type() { return TYPE; }

    public static void encode(BodyDragActionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.holding);
    }

    public static BodyDragActionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new BodyDragActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            BodyDragActionC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> BodyDragService.setHolding(
                    sender,
                    packet.targetEntityId,
                    packet.holding
            ));
        }
    }
}
