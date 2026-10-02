package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.CprService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record CprActionC2SPacket(int patientEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<CprActionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "cpr_action_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CprActionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(CprActionC2SPacket::write, CprActionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<CprActionC2SPacket> type() { return TYPE; }

    public static void encode(CprActionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static CprActionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new CprActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(CprActionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> {
                if (packet.active) {
                    CprService.start(sender, packet.patientEntityId);
                } else {
                    CprService.stop(sender, packet.patientEntityId);
                }
            });
        }
    }
}
