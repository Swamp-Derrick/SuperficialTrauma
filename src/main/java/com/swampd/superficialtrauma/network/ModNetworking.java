package com.swampd.superficialtrauma.network;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.network.packet.BloodLossFeedbackS2CPacket;
import com.swampd.superficialtrauma.network.packet.BodyStateSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.DownedPoseSyncS2CPacket;
import com.swampd.superficialtrauma.network.packet.RequestBodyStateC2SPacket;
import com.swampd.superficialtrauma.network.packet.RequestLootTargetC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "5";
    private static final long BODY_STATE_REQUEST_COOLDOWN_TICKS = 5L;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SuperficialTrauma.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int nextPacketId;
    private static final ConcurrentMap<UUID, Long> LAST_BODY_STATE_REQUEST = new ConcurrentHashMap<>();

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                BodyStateSyncS2CPacket.class,
                BodyStateSyncS2CPacket::encode,
                BodyStateSyncS2CPacket::decode,
                BodyStateSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestBodyStateC2SPacket.class,
                RequestBodyStateC2SPacket::encode,
                RequestBodyStateC2SPacket::decode,
                RequestBodyStateC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                BloodLossFeedbackS2CPacket.class,
                BloodLossFeedbackS2CPacket::encode,
                BloodLossFeedbackS2CPacket::decode,
                BloodLossFeedbackS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                DownedPoseSyncS2CPacket.class,
                DownedPoseSyncS2CPacket::encode,
                DownedPoseSyncS2CPacket::decode,
                DownedPoseSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                RequestLootTargetC2SPacket.class,
                RequestLootTargetC2SPacket::encode,
                RequestLootTargetC2SPacket::decode,
                RequestLootTargetC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void syncBodyState(ServerPlayer player) {
        BodyStateCapability.get(player).ifPresent(bodyState -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BodyStateSyncS2CPacket(bodyState.serializeNBT())
        ));
    }

    public static void syncDownedPose(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                createDownedPosePacket(player)
        );
    }

    public static void syncDownedPoseTo(ServerPlayer subject, ServerPlayer receiver) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                createDownedPosePacket(subject)
        );
    }

    public static void clearDownedPoseFor(ServerPlayer receiver, int subjectEntityId) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                DownedPoseSyncS2CPacket.active(subjectEntityId)
        );
    }

    public static void requestOwnBodyState() {
        CHANNEL.sendToServer(new RequestBodyStateC2SPacket());
    }

    public static void requestLootTarget(int targetEntityId) {
        CHANNEL.sendToServer(new RequestLootTargetC2SPacket(targetEntityId));
    }

    public static void sendBloodLossFeedback(ServerPlayer player, float amount) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BloodLossFeedbackS2CPacket(amount)
        );
    }

    public static void handleBodyStateRequest(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        Long previousRequest = LAST_BODY_STATE_REQUEST.get(player.getUUID());
        if (previousRequest != null
                && gameTime >= previousRequest
                && gameTime - previousRequest < BODY_STATE_REQUEST_COOLDOWN_TICKS) {
            return;
        }
        LAST_BODY_STATE_REQUEST.put(player.getUUID(), gameTime);
        syncBodyState(player);
    }

    public static void forgetPlayer(UUID playerId) {
        LAST_BODY_STATE_REQUEST.remove(playerId);
    }

    private static DownedPoseSyncS2CPacket createDownedPosePacket(ServerPlayer player) {
        return BodyStateCapability.get(player)
                .resolve()
                .flatMap(bodyState -> bodyState.downedPoseSnapshot())
                .map(snapshot -> DownedPoseSyncS2CPacket.downed(player.getId(), snapshot))
                .orElseGet(() -> DownedPoseSyncS2CPacket.active(player.getId()));
    }
}
