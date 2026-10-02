package com.swampd.superficialtrauma.network.packet;

import com.swampd.superficialtrauma.client.ClientDownedPoses;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.ComponentSerialization;
import com.swampd.superficialtrauma.SuperficialTrauma;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record DownedPoseSyncS2CPacket(
        int playerEntityId,
        boolean downed,
        long downedGameTime,
        float bodyYaw,
        DownedPosture posture,
        DownedFallDirection fallDirection
) implements CustomPacketPayload {
    public static final Type<DownedPoseSyncS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "downed_pose_sync_s2_cpacket"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DownedPoseSyncS2CPacket> STREAM_CODEC = StreamCodec.ofMember(DownedPoseSyncS2CPacket::write, DownedPoseSyncS2CPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<DownedPoseSyncS2CPacket> type() { return TYPE; }

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

    public static void encode(DownedPoseSyncS2CPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.playerEntityId);
        buffer.writeBoolean(packet.downed);
        if (packet.downed) {
            buffer.writeVarLong(packet.downedGameTime);
            buffer.writeFloat(packet.bodyYaw);
            buffer.writeEnum(packet.posture);
            buffer.writeEnum(packet.fallDirection);
        }
    }

    public static DownedPoseSyncS2CPacket decode(RegistryFriendlyByteBuf buffer) {
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
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientDownedPoses.update(
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
        );
    }
}
