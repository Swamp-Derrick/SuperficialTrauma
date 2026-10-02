package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientTimingQteState;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record TimingQteStartS2CPacket(TimingQteSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<TimingQteStartS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "timing_qte_start_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimingQteStartS2CPacket> STREAM_CODEC = StreamCodec.ofMember(TimingQteStartS2CPacket::write, TimingQteStartS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<TimingQteStartS2CPacket> type() { return TYPE; }

    public static void encode(TimingQteStartS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        TimingQteSnapshot snapshot = packet.snapshot;
        buffer.writeVarInt(snapshot.sessionId());
        buffer.writeLong(snapshot.cursorStartGameTime());
        buffer.writeVarInt(snapshot.sweepDurationTicks());
        buffer.writeFloat(snapshot.perfectStart());
        buffer.writeFloat(snapshot.normalStart());
        buffer.writeFloat(snapshot.successEnd());
    }

    public static TimingQteStartS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new TimingQteStartS2CPacket(new TimingQteSnapshot(
                buffer.readVarInt(),
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        ));
    }

    public static void handle(TimingQteStartS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientTimingQteState.start(packet.snapshot)
        );
    }
}
