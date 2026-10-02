package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record StartTreatmentC2SPacket(
        int patientEntityId,
        UUID woundId,
        TreatmentProcedure procedure,
        TreatmentAction action
) implements CustomPacketPayload {
    public static final Type<StartTreatmentC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "start_treatment_c2_spacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartTreatmentC2SPacket> STREAM_CODEC = StreamCodec.ofMember(StartTreatmentC2SPacket::write, StartTreatmentC2SPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<StartTreatmentC2SPacket> type() { return TYPE; }

    public static void encode(StartTreatmentC2SPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeUtf(packet.procedure.serializedName());
        buffer.writeUtf(packet.action.serializedName());
    }

    public static StartTreatmentC2SPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StartTreatmentC2SPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                TreatmentProcedure.fromSerializedName(buffer.readUtf(64)),
                TreatmentAction.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartTreatmentC2SPacket packet, IPayloadContext context) {
        ServerPlayer sender = (context.player() instanceof ServerPlayer player ? player : null);
        if (sender != null) {
            context.enqueueWork(() -> TreatmentService.start(
                    sender,
                    packet.patientEntityId,
                    packet.woundId,
                    packet.procedure,
                    packet.action
            ));
        }
    }
}
