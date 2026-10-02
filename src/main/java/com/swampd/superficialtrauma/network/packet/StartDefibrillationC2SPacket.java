package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.treatment.DefibrillationAction;
import com.swampd.superficialtrauma.common.treatment.DefibrillationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record StartDefibrillationC2SPacket(
        int patientEntityId,
        int joules,
        DefibrillationAction action
) implements CustomPacketPayload {
    public static final Type<StartDefibrillationC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "start_defibrillation_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartDefibrillationC2SPacket> STREAM_CODEC = StreamCodec.ofMember(StartDefibrillationC2SPacket::write, StartDefibrillationC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<StartDefibrillationC2SPacket> type() { return TYPE; }

    public static void encode(StartDefibrillationC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeVarInt(packet.joules);
        buffer.writeEnum(packet.action);
    }

    public static StartDefibrillationC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StartDefibrillationC2SPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readEnum(DefibrillationAction.class)
        );
    }

    public static void handle(
            StartDefibrillationC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> {
                DefibrillationEnergy energy = DefibrillationEnergy.fromJoules(packet.joules);
                switch (packet.action) {
                    case START -> DefibrillationService.start(sender, packet.patientEntityId, energy);
                    case RELEASE -> DefibrillationService.release(sender, packet.patientEntityId);
                    case CANCEL -> DefibrillationService.cancel(sender, packet.patientEntityId);
                }
            });
        }
    }
}
