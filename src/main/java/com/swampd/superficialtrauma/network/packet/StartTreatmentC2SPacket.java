package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record StartTreatmentC2SPacket(
        int patientEntityId,
        UUID woundId,
        TreatmentProcedure procedure,
        TreatmentAction action
) {
    public static void encode(StartTreatmentC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUUID(packet.woundId);
        buffer.writeUtf(packet.procedure.serializedName());
        buffer.writeUtf(packet.action.serializedName());
    }

    public static StartTreatmentC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartTreatmentC2SPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                TreatmentProcedure.fromSerializedName(buffer.readUtf(64)),
                TreatmentAction.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartTreatmentC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> TreatmentService.start(
                    sender,
                    packet.patientEntityId,
                    packet.woundId,
                    packet.procedure,
                    packet.action
            ));
        }
        context.setPacketHandled(true);
    }
}
