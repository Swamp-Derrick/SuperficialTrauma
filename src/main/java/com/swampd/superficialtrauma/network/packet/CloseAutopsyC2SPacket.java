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


public record CloseAutopsyC2SPacket(int corpseEntityId) implements CustomPacketPayload {
    public static final Type<CloseAutopsyC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "close_autopsy_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseAutopsyC2SPacket> STREAM_CODEC = StreamCodec.ofMember(CloseAutopsyC2SPacket::write, CloseAutopsyC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CloseAutopsyC2SPacket> type() { return TYPE; }

    public static void encode(CloseAutopsyC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
    }

    public static CloseAutopsyC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CloseAutopsyC2SPacket(buffer.readVarInt());
    }

    public static void handle(CloseAutopsyC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> AutopsyService.close(sender, packet.corpseEntityId));
        }
    }
}
