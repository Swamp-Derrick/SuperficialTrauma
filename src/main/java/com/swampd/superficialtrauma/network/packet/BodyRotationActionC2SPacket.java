package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.drag.BodyRotationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record BodyRotationActionC2SPacket(int targetEntityId, boolean holding) implements CustomPacketPayload {
    public static final Type<BodyRotationActionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "body_rotation_action_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyRotationActionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(BodyRotationActionC2SPacket::write, BodyRotationActionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BodyRotationActionC2SPacket> type() { return TYPE; }

    public static void encode(BodyRotationActionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.holding);
    }

    public static BodyRotationActionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new BodyRotationActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            BodyRotationActionC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> BodyRotationService.setHolding(
                    sender,
                    packet.targetEntityId,
                    packet.holding
            ));
        }
    }
}
