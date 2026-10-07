package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.qte.TimingQteService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Acknowledges the first displayed QTE frame, once per session. */
public record TimingQteReadyC2SPacket(int sessionId) implements CustomPacketPayload {
    public static final Type<TimingQteReadyC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID, "timing_qte_ready"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimingQteReadyC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> buffer.writeVarInt(packet.sessionId),
            buffer -> new TimingQteReadyC2SPacket(buffer.readVarInt()));
    @Override public Type<TimingQteReadyC2SPacket> type() { return TYPE; }

    public static void handle(TimingQteReadyC2SPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> TimingQteService.ready(player, packet.sessionId));
        }
    }
}
