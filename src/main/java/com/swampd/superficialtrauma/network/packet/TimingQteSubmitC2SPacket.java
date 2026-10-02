package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.qte.TimingQteService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record TimingQteSubmitC2SPacket(int sessionId, float elapsedTicks, boolean pressed) implements CustomPacketPayload {
    public static final Type<TimingQteSubmitC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "timing_qte_submit_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimingQteSubmitC2SPacket> STREAM_CODEC = StreamCodec.ofMember(TimingQteSubmitC2SPacket::write, TimingQteSubmitC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<TimingQteSubmitC2SPacket> type() { return TYPE; }

    public static void encode(TimingQteSubmitC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeFloat(packet.elapsedTicks);
        buffer.writeBoolean(packet.pressed);
    }

    public static TimingQteSubmitC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new TimingQteSubmitC2SPacket(
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(TimingQteSubmitC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> TimingQteService.submit(
                    sender,
                    packet.sessionId,
                    packet.elapsedTicks,
                    packet.pressed
            ));
        }
    }
}
