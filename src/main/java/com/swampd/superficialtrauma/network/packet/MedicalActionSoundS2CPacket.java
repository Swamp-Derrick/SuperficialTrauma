package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicalActionSounds;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record MedicalActionSoundS2CPacket(
        int actorEntityId,
        MedicalActionSoundChannel channel,
        MedicalActionSound sound,
        Operation operation
) implements CustomPacketPayload {
    public static final Type<MedicalActionSoundS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "medical_action_sound_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicalActionSoundS2CPacket> STREAM_CODEC = StreamCodec.ofMember(MedicalActionSoundS2CPacket::write, MedicalActionSoundS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MedicalActionSoundS2CPacket> type() { return TYPE; }

    public static MedicalActionSoundS2CPacket start(
            int actorEntityId,
            MedicalActionSoundChannel channel,
            MedicalActionSound sound
    ) {
        return new MedicalActionSoundS2CPacket(actorEntityId, channel, sound, Operation.START);
    }

    public static MedicalActionSoundS2CPacket stop(
            int actorEntityId,
            MedicalActionSoundChannel channel
    ) {
        return new MedicalActionSoundS2CPacket(
                actorEntityId,
                channel,
                MedicalActionSound.CLOTH_WRAPPING,
                Operation.STOP
        );
    }

    public static MedicalActionSoundS2CPacket playOnce(int actorEntityId, MedicalActionSound sound) {
        return new MedicalActionSoundS2CPacket(
                actorEntityId,
                MedicalActionSoundChannel.TREATMENT,
                sound,
                Operation.PLAY_ONCE
        );
    }

    public static void encode(MedicalActionSoundS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actorEntityId);
        buffer.writeEnum(packet.channel);
        buffer.writeEnum(packet.sound);
        buffer.writeEnum(packet.operation);
    }

    public static MedicalActionSoundS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MedicalActionSoundS2CPacket(
                buffer.readVarInt(),
                buffer.readEnum(MedicalActionSoundChannel.class),
                buffer.readEnum(MedicalActionSound.class),
                buffer.readEnum(Operation.class)
        );
    }

    public static void handle(
            MedicalActionSoundS2CPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            switch (packet.operation) {
                case START -> ClientMedicalActionSounds.start(
                        packet.actorEntityId,
                        packet.channel,
                        packet.sound
                );
                case STOP -> ClientMedicalActionSounds.stop(packet.actorEntityId, packet.channel);
                case PLAY_ONCE -> ClientMedicalActionSounds.playOnce(packet.actorEntityId, packet.sound);
            }
        });
    }

    public enum Operation {
        START,
        STOP,
        PLAY_ONCE
    }
}
