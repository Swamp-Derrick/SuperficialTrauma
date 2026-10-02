package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTimingQteState;
import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record TimingQteResultS2CPacket(int sessionId, TimingQteResult result) implements CustomPacketPayload {
    public static final Type<TimingQteResultS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "timing_qte_result_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimingQteResultS2CPacket> STREAM_CODEC = StreamCodec.ofMember(TimingQteResultS2CPacket::write, TimingQteResultS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<TimingQteResultS2CPacket> type() { return TYPE; }

    public static void encode(TimingQteResultS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeUtf(packet.result.serializedName());
    }

    public static TimingQteResultS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new TimingQteResultS2CPacket(
                buffer.readVarInt(),
                TimingQteResult.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(TimingQteResultS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientTimingQteState.resolve(packet.sessionId, packet.result)
        );
    }
}
