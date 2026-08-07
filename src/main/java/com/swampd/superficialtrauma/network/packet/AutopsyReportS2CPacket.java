package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientAutopsyState;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AutopsyReportS2CPacket(CompoundTag reportTag, boolean openScreen) {
    public AutopsyReportS2CPacket {
        reportTag = reportTag == null ? new CompoundTag() : reportTag.copy();
    }

    public static void encode(AutopsyReportS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.reportTag);
        buffer.writeBoolean(packet.openScreen);
    }

    public static AutopsyReportS2CPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new AutopsyReportS2CPacket(tag == null ? new CompoundTag() : tag, buffer.readBoolean());
    }

    public static void handle(AutopsyReportS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAutopsyState.update(AutopsyReport.load(packet.reportTag), packet.openScreen)
        ));
        context.setPacketHandled(true);
    }
}
