package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicalInspectionNotice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MedicalInspectionNoticeS2CPacket(
        String inspectorName,
        Component actionItemName
) {
    public static void encode(MedicalInspectionNoticeS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.inspectorName, 64);
        buffer.writeBoolean(packet.actionItemName != null);
        if (packet.actionItemName != null) {
            buffer.writeComponent(packet.actionItemName);
        }
    }

    public static MedicalInspectionNoticeS2CPacket decode(FriendlyByteBuf buffer) {
        String inspectorName = buffer.readUtf(64);
        Component actionItemName = buffer.readBoolean() ? buffer.readComponent() : null;
        return new MedicalInspectionNoticeS2CPacket(inspectorName, actionItemName);
    }

    public static void handle(
            MedicalInspectionNoticeS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientMedicalInspectionNotice.update(packet.inspectorName, packet.actionItemName)
        ));
        context.setPacketHandled(true);
    }
}
