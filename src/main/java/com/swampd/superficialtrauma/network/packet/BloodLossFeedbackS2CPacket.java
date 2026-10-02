package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientBloodLossOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record BloodLossFeedbackS2CPacket(float amount) implements CustomPacketPayload {
    public static final Type<BloodLossFeedbackS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "blood_loss_feedback_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BloodLossFeedbackS2CPacket> STREAM_CODEC = StreamCodec.ofMember(BloodLossFeedbackS2CPacket::write, BloodLossFeedbackS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BloodLossFeedbackS2CPacket> type() { return TYPE; }

    public BloodLossFeedbackS2CPacket {
        amount = Math.max(0.0F, amount);
    }

    public static void encode(BloodLossFeedbackS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(packet.amount);
    }

    public static BloodLossFeedbackS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new BloodLossFeedbackS2CPacket(buffer.readFloat());
    }

    public static void handle(
            BloodLossFeedbackS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientBloodLossOverlay.trigger(packet.amount)
        );
    }
}
