package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.AirwayService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record AirwayActionC2SPacket(int patientEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<AirwayActionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "airway_action_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirwayActionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(AirwayActionC2SPacket::write, AirwayActionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<AirwayActionC2SPacket> type() { return TYPE; }

    public static void encode(AirwayActionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static AirwayActionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new AirwayActionC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(AirwayActionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> {
                if (packet.active) {
                    AirwayService.start(sender, packet.patientEntityId);
                } else {
                    AirwayService.stop(sender, packet.patientEntityId);
                }
            });
        }
    }
}
