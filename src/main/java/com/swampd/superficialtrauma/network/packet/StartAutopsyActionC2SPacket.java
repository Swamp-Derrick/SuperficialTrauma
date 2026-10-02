package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record StartAutopsyActionC2SPacket(int corpseEntityId, AutopsyAction action) implements CustomPacketPayload {
    public static final Type<StartAutopsyActionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "start_autopsy_action_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartAutopsyActionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(StartAutopsyActionC2SPacket::write, StartAutopsyActionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<StartAutopsyActionC2SPacket> type() { return TYPE; }

    public StartAutopsyActionC2SPacket {
        action = action == null ? AutopsyAction.NONE : action;
    }

    public static void encode(StartAutopsyActionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.corpseEntityId);
        buffer.writeUtf(packet.action.serializedName());
    }

    public static StartAutopsyActionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StartAutopsyActionC2SPacket(
                buffer.readVarInt(),
                AutopsyAction.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartAutopsyActionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> AutopsyService.start(sender, packet.corpseEntityId, packet.action));
        }
    }
}
