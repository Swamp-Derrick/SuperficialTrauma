package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.medication.MedicationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record MedicationPreparationC2SPacket(int patientEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<MedicationPreparationC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "medication_preparation_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicationPreparationC2SPacket> STREAM_CODEC = StreamCodec.ofMember(MedicationPreparationC2SPacket::write, MedicationPreparationC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MedicationPreparationC2SPacket> type() { return TYPE; }

    public static void encode(MedicationPreparationC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeBoolean(packet.active);
    }

    public static MedicationPreparationC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MedicationPreparationC2SPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            MedicationPreparationC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> MedicationService.setInjectionPreparation(
                    sender,
                    packet.patientEntityId,
                    packet.active
            ));
        }
    }
}
