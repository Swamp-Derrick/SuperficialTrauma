package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.loot.LootingService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record RequestLootTargetC2SPacket(int targetEntityId) implements CustomPacketPayload {
    public static final Type<RequestLootTargetC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "request_loot_target_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLootTargetC2SPacket> STREAM_CODEC = StreamCodec.ofMember(RequestLootTargetC2SPacket::write, RequestLootTargetC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<RequestLootTargetC2SPacket> type() { return TYPE; }

    public static void encode(RequestLootTargetC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static RequestLootTargetC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RequestLootTargetC2SPacket(buffer.readVarInt());
    }

    public static void handle(
            RequestLootTargetC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> LootingService.tryOpen(sender, packet.targetEntityId));
        }
    }
}
