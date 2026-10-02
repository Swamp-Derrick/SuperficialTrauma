package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientGiveUpState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record GiveUpSessionS2CPacket(Status status, long endsGameTime) implements CustomPacketPayload {
    public static final Type<GiveUpSessionS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "give_up_session_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GiveUpSessionS2CPacket> STREAM_CODEC = StreamCodec.ofMember(GiveUpSessionS2CPacket::write, GiveUpSessionS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<GiveUpSessionS2CPacket> type() { return TYPE; }

    public static GiveUpSessionS2CPacket started(long endsGameTime) {
        return new GiveUpSessionS2CPacket(Status.STARTED, endsGameTime);
    }

    public static GiveUpSessionS2CPacket cancelled() {
        return new GiveUpSessionS2CPacket(Status.CANCELLED, -1L);
    }

    public static GiveUpSessionS2CPacket completed() {
        return new GiveUpSessionS2CPacket(Status.COMPLETED, -1L);
    }

    public static void encode(GiveUpSessionS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status);
        buffer.writeLong(packet.endsGameTime);
    }

    public static GiveUpSessionS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new GiveUpSessionS2CPacket(buffer.readEnum(Status.class), buffer.readLong());
    }

    public static void handle(
            GiveUpSessionS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (packet.status == Status.STARTED) {
                ClientGiveUpState.started(packet.endsGameTime);
            } else {
                ClientGiveUpState.clear();
            }
        });
    }

    public enum Status {
        STARTED,
        CANCELLED,
        COMPLETED
    }
}
