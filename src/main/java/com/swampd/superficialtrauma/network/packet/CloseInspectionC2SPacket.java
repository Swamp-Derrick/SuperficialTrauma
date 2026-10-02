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


public record CloseInspectionC2SPacket(int targetEntityId) implements CustomPacketPayload {
    public static final Type<CloseInspectionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "close_inspection_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseInspectionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(CloseInspectionC2SPacket::write, CloseInspectionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CloseInspectionC2SPacket> type() { return TYPE; }

    public static void encode(CloseInspectionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static CloseInspectionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CloseInspectionC2SPacket(buffer.readVarInt());
    }

    public static void handle(CloseInspectionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> InspectionService.close(sender, packet.targetEntityId));
        }
    }
}
