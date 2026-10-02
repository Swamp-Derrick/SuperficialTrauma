package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.medication.MedicationService;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record StartMedicationC2SPacket(int patientEntityId, MedicationType selectedType) implements CustomPacketPayload {
    public static final Type<StartMedicationC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "start_medication_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartMedicationC2SPacket> STREAM_CODEC = StreamCodec.ofMember(StartMedicationC2SPacket::write, StartMedicationC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<StartMedicationC2SPacket> type() { return TYPE; }

    public static void encode(StartMedicationC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.selectedType.serializedName());
    }

    public static StartMedicationC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StartMedicationC2SPacket(
                buffer.readVarInt(),
                MedicationType.fromNetworkName(buffer.readUtf(32)).orElse(null)
        );
    }

    public static void handle(StartMedicationC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null && packet.selectedType != null) {
            context.enqueueWork(() -> MedicationService.start(sender, packet.patientEntityId, packet.selectedType));
        }
    }
}
