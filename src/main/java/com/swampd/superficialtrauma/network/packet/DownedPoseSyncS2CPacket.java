package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientDownedPoses;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DownedPoseSyncS2CPacket(
        int playerEntityId,
        boolean downed,
        long downedGameTime,
        float bodyYaw,
        DownedPosture posture,
        DownedFallDirection fallDirection
) {
    public static DownedPoseSyncS2CPacket active(int playerEntityId) {
        return new DownedPoseSyncS2CPacket(
                playerEntityId,
                false,
                0L,
                0.0F,
                DownedPosture.UNSAFE,
                DownedFallDirection.FADE_ONLY
        );
    }

    public static DownedPoseSyncS2CPacket downed(int playerEntityId, DownedPoseSnapshot snapshot) {
        return new DownedPoseSyncS2CPacket(
                playerEntityId,
                true,
                snapshot.downedGameTime(),
                snapshot.bodyYaw(),
                snapshot.posture(),
                snapshot.fallDirection()
        );
    }

    public static void encode(DownedPoseSyncS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.playerEntityId);
        buffer.writeBoolean(packet.downed);
        if (packet.downed) {
            buffer.writeVarLong(packet.downedGameTime);
            buffer.writeFloat(packet.bodyYaw);
            buffer.writeEnum(packet.posture);
            buffer.writeEnum(packet.fallDirection);
        }
    }

    public static DownedPoseSyncS2CPacket decode(FriendlyByteBuf buffer) {
        int playerEntityId = buffer.readVarInt();
        if (!buffer.readBoolean()) {
            return active(playerEntityId);
        }
        return new DownedPoseSyncS2CPacket(
                playerEntityId,
                true,
                buffer.readVarLong(),
                buffer.readFloat(),
                buffer.readEnum(DownedPosture.class),
                buffer.readEnum(DownedFallDirection.class)
        );
    }

    public static void handle(
            DownedPoseSyncS2CPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDownedPoses.update(
                        packet.playerEntityId,
                        packet.downed
                                ? new DownedPoseSnapshot(
                                        packet.downedGameTime,
                                        packet.bodyYaw,
                                        packet.posture,
                                        packet.fallDirection
                                )
                                : null
                )
        ));
        context.setPacketHandled(true);
    }
}
