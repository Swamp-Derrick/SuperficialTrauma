package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientMedicalActionSounds;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MedicalActionSoundS2CPacket(
        int actorEntityId,
        MedicalActionSoundChannel channel,
        MedicalActionSound sound,
        Operation operation
) {
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

    public static void encode(MedicalActionSoundS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.actorEntityId);
        buffer.writeEnum(packet.channel);
        buffer.writeEnum(packet.sound);
        buffer.writeEnum(packet.operation);
    }

    public static MedicalActionSoundS2CPacket decode(FriendlyByteBuf buffer) {
        return new MedicalActionSoundS2CPacket(
                buffer.readVarInt(),
                buffer.readEnum(MedicalActionSoundChannel.class),
                buffer.readEnum(MedicalActionSound.class),
                buffer.readEnum(Operation.class)
        );
    }

    public static void handle(
            MedicalActionSoundS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (packet.operation) {
                case START -> ClientMedicalActionSounds.start(
                        packet.actorEntityId,
                        packet.channel,
                        packet.sound
                );
                case STOP -> ClientMedicalActionSounds.stop(packet.actorEntityId, packet.channel);
                case PLAY_ONCE -> ClientMedicalActionSounds.playOnce(packet.actorEntityId, packet.sound);
            }
        }));
        context.setPacketHandled(true);
    }

    public enum Operation {
        START,
        STOP,
        PLAY_ONCE
    }
}
