package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.GiveUpService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record GiveUpHoldC2SPacket(boolean holding) implements CustomPacketPayload {
    public static final Type<GiveUpHoldC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "give_up_hold_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GiveUpHoldC2SPacket> STREAM_CODEC = StreamCodec.ofMember(GiveUpHoldC2SPacket::write, GiveUpHoldC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<GiveUpHoldC2SPacket> type() { return TYPE; }

    public static void encode(GiveUpHoldC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.holding);
    }

    public static GiveUpHoldC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new GiveUpHoldC2SPacket(buffer.readBoolean());
    }

    public static void handle(GiveUpHoldC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> GiveUpService.setHolding(sender, packet.holding));
        }
    }
}
