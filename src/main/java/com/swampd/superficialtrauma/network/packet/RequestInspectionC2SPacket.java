package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.InspectionService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record RequestInspectionC2SPacket(int targetEntityId) implements CustomPacketPayload {
    public static final Type<RequestInspectionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "request_inspection_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestInspectionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(RequestInspectionC2SPacket::write, RequestInspectionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<RequestInspectionC2SPacket> type() { return TYPE; }

    public static void encode(RequestInspectionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static RequestInspectionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RequestInspectionC2SPacket(buffer.readVarInt());
    }

    public static void handle(RequestInspectionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> InspectionService.open(sender, packet.targetEntityId));
        }
    }
}
