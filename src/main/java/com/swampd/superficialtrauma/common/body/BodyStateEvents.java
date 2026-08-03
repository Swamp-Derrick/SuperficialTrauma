package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BodyStateEvents {
    private BodyStateEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            BodyStateProvider provider = new BodyStateProvider();
            event.addCapability(BodyStateCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return;
        }

        event.getOriginal().reviveCaps();
        BodyStateCapability.get(event.getOriginal()).ifPresent(oldState ->
                BodyStateCapability.get(event.getEntity()).ifPresent(newState -> newState.copyFrom(oldState))
        );
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BodyStateCapability.get(serverPlayer).ifPresent(bodyState ->
                    bodyState.pauseBodyProgression(serverPlayer.serverLevel().getGameTime())
            );
        }
        ModNetworking.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        resumeProgressionIfServerPlayer(event.getEntity());
        syncIfServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        long gameTime = serverPlayer.serverLevel().getGameTime();
        BodyStateCapability.get(serverPlayer).ifPresent(bodyState -> {
            BodyProgressionResult result = bodyState.advanceBodyProgression(gameTime);
            if (result.changed()) {
                ModNetworking.syncBodyState(serverPlayer);
            }
        });
    }

    private static void syncIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.syncBodyState(serverPlayer);
        }
    }

    private static void resumeProgressionIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            BodyStateCapability.get(serverPlayer).ifPresent(bodyState ->
                    bodyState.resumeBodyProgression(serverPlayer.serverLevel().getGameTime())
            );
        }
    }
}
