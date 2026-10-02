package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationSoundService;
import com.swampd.superficialtrauma.common.treatment.TreatmentPreparationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TreatmentPreparationSoundC2SPacket(
        int patientEntityId,
        UUID woundId,
        TreatmentPreparationType selectedType,
        boolean active
) implements CustomPacketPayload {
    public static final Type<TreatmentPreparationSoundC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "treatment_preparation_sound_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TreatmentPreparationSoundC2SPacket> STREAM_CODEC = StreamCodec.ofMember(TreatmentPreparationSoundC2SPacket::write, TreatmentPreparationSoundC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<TreatmentPreparationSoundC2SPacket> type() { return TYPE; }

    public static void encode(TreatmentPreparationSoundC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeEnum(packet.selectedType);
        buffer.writeBoolean(packet.active);
    }

    public static TreatmentPreparationSoundC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new TreatmentPreparationSoundC2SPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readEnum(TreatmentPreparationType.class),
                buffer.readBoolean()
        );
    }

    public static void handle(
            TreatmentPreparationSoundC2SPacket packet,
            IPayloadContext context
    ) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> TreatmentPreparationSoundService.set(
                    sender,
                    packet.patientEntityId,
                    packet.woundId,
                    packet.selectedType,
                    packet.active
            ));
        }
    }
}
