package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.treatment.InfusionService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record StartInfusionC2SPacket(int patientEntityId, InfusionType selectedType) implements CustomPacketPayload {
    public static final Type<StartInfusionC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "start_infusion_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartInfusionC2SPacket> STREAM_CODEC = StreamCodec.ofMember(StartInfusionC2SPacket::write, StartInfusionC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<StartInfusionC2SPacket> type() { return TYPE; }

    public static void encode(StartInfusionC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.selectedType.serializedName());
    }

    public static StartInfusionC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StartInfusionC2SPacket(
                buffer.readVarInt(),
                InfusionType.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartInfusionC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> InfusionService.start(sender, packet.patientEntityId, packet.selectedType));
        }
    }
}
