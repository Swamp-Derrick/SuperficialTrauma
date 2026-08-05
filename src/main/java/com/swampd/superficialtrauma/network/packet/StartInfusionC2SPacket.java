package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.treatment.InfusionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StartInfusionC2SPacket(int patientEntityId, InfusionType type) {
    public static void encode(StartInfusionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.patientEntityId);
        buffer.writeUtf(packet.type.serializedName());
    }

    public static StartInfusionC2SPacket decode(FriendlyByteBuf buffer) {
        return new StartInfusionC2SPacket(
                buffer.readVarInt(),
                InfusionType.fromSerializedName(buffer.readUtf(32))
        );
    }

    public static void handle(StartInfusionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> InfusionService.start(sender, packet.patientEntityId, packet.type));
        }
        context.setPacketHandled(true);
    }
}
