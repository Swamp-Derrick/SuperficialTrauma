package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBloodLossOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BloodLossFeedbackS2CPacket(float amount) {
    public BloodLossFeedbackS2CPacket {
        amount = Math.max(0.0F, amount);
    }

    public static void encode(BloodLossFeedbackS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.amount);
    }

    public static BloodLossFeedbackS2CPacket decode(FriendlyByteBuf buffer) {
        return new BloodLossFeedbackS2CPacket(buffer.readFloat());
    }

    public static void handle(
            BloodLossFeedbackS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBloodLossOverlay.trigger(packet.amount)
        ));
        context.setPacketHandled(true);
    }
}
