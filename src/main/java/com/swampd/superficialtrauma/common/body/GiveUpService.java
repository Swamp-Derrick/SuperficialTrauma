package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GiveUpService {
    public static final long HOLD_DURATION_TICKS = 5L * 20L;
    private static final long HEARTBEAT_GRACE_TICKS = 15L;
    private static final Map<UUID, GiveUpSession> SESSIONS = new HashMap<>();

    private GiveUpService() {
    }

    public static void setHolding(ServerPlayer player, boolean holding) {
        if (!holding) {
            cancel(player, true);
            return;
        }
        BodyState state = BodyStateCapability.get(player).orElse(null);
        if (!isEligible(player, state)) {
            cancel(player, true);
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        GiveUpSession existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            SESSIONS.put(
                    player.getUUID(),
                    new GiveUpSession(existing.endsGameTime(), gameTime)
            );
            return;
        }

        GiveUpSession created = new GiveUpSession(
                gameTime + HOLD_DURATION_TICKS,
                gameTime
        );
        SESSIONS.put(player.getUUID(), created);
        ModNetworking.sendGiveUpStarted(player, created.endsGameTime());
    }

    public static void tick(ServerPlayer player) {
        GiveUpSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        BodyState state = BodyStateCapability.get(player).orElse(null);
        if (!isEligible(player, state)
                || gameTime - session.lastHeartbeatGameTime() > HEARTBEAT_GRACE_TICKS) {
            cancel(player, true);
            return;
        }
        if (gameTime < session.endsGameTime()) {
            return;
        }

        SESSIONS.remove(player.getUUID());
        if (state != null && state.giveUp()) {
            ModNetworking.sendGiveUpCompleted(player);
            ModNetworking.syncBodyState(player);
        } else {
            ModNetworking.sendGiveUpCancelled(player);
        }
    }

    public static boolean isEligible(BodyState state) {
        return state != null && switch (state.lifeState()) {
            case INCAPACITATED, CARDIAC_ARREST, VENTRICULAR_FIBRILLATION -> true;
            case ACTIVE, AWAKENING, BRAIN_DEAD -> false;
        };
    }

    public static void forgetPlayer(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    private static boolean isEligible(ServerPlayer player, BodyState state) {
        return player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && isEligible(state);
    }

    private static void cancel(ServerPlayer player, boolean notifyClient) {
        GiveUpSession removed = SESSIONS.remove(player.getUUID());
        if (removed != null && notifyClient) {
            ModNetworking.sendGiveUpCancelled(player);
        }
    }

    private record GiveUpSession(long endsGameTime, long lastHeartbeatGameTime) {
    }
}
