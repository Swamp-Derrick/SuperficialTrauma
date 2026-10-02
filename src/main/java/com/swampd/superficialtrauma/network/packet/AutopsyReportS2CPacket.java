package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientAutopsyState;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record AutopsyReportS2CPacket(CompoundTag reportTag, boolean openScreen) implements CustomPacketPayload {
    public static final Type<AutopsyReportS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "autopsy_report_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AutopsyReportS2CPacket> STREAM_CODEC = StreamCodec.ofMember(AutopsyReportS2CPacket::write, AutopsyReportS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<AutopsyReportS2CPacket> type() { return TYPE; }

    public AutopsyReportS2CPacket {
        reportTag = reportTag == null ? new CompoundTag() : reportTag.copy();
    }

    public static void encode(AutopsyReportS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeNbt(packet.reportTag);
        buffer.writeBoolean(packet.openScreen);
    }

    public static AutopsyReportS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new AutopsyReportS2CPacket(tag == null ? new CompoundTag() : tag, buffer.readBoolean());
    }

    public static void handle(AutopsyReportS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientAutopsyState.update(AutopsyReport.load(packet.reportTag), packet.openScreen)
        );
    }
}
