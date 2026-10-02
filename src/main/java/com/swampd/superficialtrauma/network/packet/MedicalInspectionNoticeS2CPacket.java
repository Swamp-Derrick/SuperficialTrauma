package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicalInspectionNotice;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record MedicalInspectionNoticeS2CPacket(
        String inspectorName,
        Component actionItemName
) implements CustomPacketPayload {
    public static final Type<MedicalInspectionNoticeS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "medical_inspection_notice_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicalInspectionNoticeS2CPacket> STREAM_CODEC = StreamCodec.ofMember(MedicalInspectionNoticeS2CPacket::write, MedicalInspectionNoticeS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MedicalInspectionNoticeS2CPacket> type() { return TYPE; }

    public static void encode(MedicalInspectionNoticeS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(packet.inspectorName, 64);
        buffer.writeBoolean(packet.actionItemName != null);
        if (packet.actionItemName != null) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, packet.actionItemName);
        }
    }

    public static MedicalInspectionNoticeS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        String inspectorName = buffer.readUtf(64);
        Component actionItemName = buffer.readBoolean() ? ComponentSerialization.STREAM_CODEC.decode(buffer) : null;
        return new MedicalInspectionNoticeS2CPacket(inspectorName, actionItemName);
    }

    public static void handle(
            MedicalInspectionNoticeS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() ->
                ClientMedicalInspectionNotice.update(packet.inspectorName, packet.actionItemName)
        );
    }
}
