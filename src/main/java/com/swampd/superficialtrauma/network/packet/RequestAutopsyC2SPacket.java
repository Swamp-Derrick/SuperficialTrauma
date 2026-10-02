package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record RequestAutopsyC2SPacket(int corpseEntityId) implements CustomPacketPayload {
    public static final Type<RequestAutopsyC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "request_autopsy_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAutopsyC2SPacket> STREAM_CODEC = StreamCodec.ofMember(RequestAutopsyC2SPacket::write, RequestAutopsyC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<RequestAutopsyC2SPacket> type() { return TYPE; }

    public static void encode(RequestAutopsyC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
    }

    public static RequestAutopsyC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RequestAutopsyC2SPacket(buffer.readVarInt());
    }

    public static void handle(RequestAutopsyC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> AutopsyService.open(sender, packet.corpseEntityId));
        }
    }
}
