package com.swampd.superficialtrauma.common.voice;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class ServerVoicechatState {
    private ServerVoicechatState() {}

    public static void refresh(ServerPlayer player) {
        BodyStateCapability.get(player).ifPresent(body -> {
            long now = player.serverLevel().getGameTime();
            var previous = VoicechatStates.server(player.getUUID());
            long since = body.downedPoseSnapshot().map(DownedPoseSnapshot::downedGameTime)
                    .orElse(previous.downed() ? previous.downedSince() : now);
            VoicechatStates.publishServer(player.getUUID(), DownedVoiceState.update(body.lifeState(), since, now, previous));
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { VoicechatStates.forgetServer(event.getEntity().getUUID()); }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { VoicechatStates.clearServer(); }
}
