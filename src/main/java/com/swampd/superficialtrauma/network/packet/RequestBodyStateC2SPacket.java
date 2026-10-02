package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record RequestBodyStateC2SPacket() implements CustomPacketPayload {
    public static final Type<RequestBodyStateC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "request_body_state_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBodyStateC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestBodyStateC2SPacket());
    @Override public Type<RequestBodyStateC2SPacket> type() { return TYPE; }
    public static void encode(RequestBodyStateC2SPacket packet, RegistryFriendlyByteBuf buffer) {
    }

    public static RequestBodyStateC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RequestBodyStateC2SPacket();
    }

    public static void handle(
            RequestBodyStateC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> ModNetworking.handleBodyStateRequest(sender));
        }
    }
}
