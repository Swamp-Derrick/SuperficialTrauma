package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientDefibrillatorSounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record DefibrillatorChargingSoundS2CPacket(int actorEntityId, boolean charging) implements CustomPacketPayload {
    public static final Type<DefibrillatorChargingSoundS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "defibrillator_charging_sound_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DefibrillatorChargingSoundS2CPacket> STREAM_CODEC = StreamCodec.ofMember(DefibrillatorChargingSoundS2CPacket::write, DefibrillatorChargingSoundS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<DefibrillatorChargingSoundS2CPacket> type() { return TYPE; }

    public static void encode(DefibrillatorChargingSoundS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actorEntityId);
        buffer.writeBoolean(packet.charging);
    }

    public static DefibrillatorChargingSoundS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new DefibrillatorChargingSoundS2CPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            DefibrillatorChargingSoundS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientDefibrillatorSounds.setCharging(
                        packet.actorEntityId,
                        packet.charging
                )
        );
    }
}
