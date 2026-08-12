package com.swampd.superficialtrauma.common.qte;

import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative owner of reusable single-player timing QTE sessions. */
public final class TimingQteService {
    private static final float MAX_REPORTED_FUTURE_TICKS = 2.0F;
    private static final float MAX_REPORTED_AGE_TICKS = 20.0F;
    private static final Map<UUID, ActiveSession> ACTIVE = new HashMap<>();
    private static int nextSessionId = 1;

    private TimingQteService() {
    }

    public static boolean start(
            ServerPlayer player,
            TimingQteDefinition definition,
            ResultHandler resultHandler
    ) {
        if (ACTIVE.containsKey(player.getUUID())) {
            return false;
        }
        float perfectStart = Mth.lerp(
                player.getRandom().nextFloat(),
                definition.earliestPerfectStart(),
                definition.latestPerfectStart()
        );
        float normalStart = perfectStart + definition.perfectArcWidth();
        float successEnd = normalStart + definition.successArcWidth();
        int sessionId = nextSessionId();
        long cursorStart = player.serverLevel().getGameTime() + definition.leadInTicks();
        TimingQteSnapshot snapshot = new TimingQteSnapshot(
                sessionId,
                cursorStart,
                definition.sweepDurationTicks(),
                perfectStart,
                normalStart,
                successEnd
        );
        ACTIVE.put(player.getUUID(), new ActiveSession(
                snapshot,
                definition.serverResponseGraceTicks(),
                resultHandler
        ));
        ModNetworking.sendTimingQteStarted(player, snapshot);
        return true;
    }

    public static boolean hasActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void submit(
            ServerPlayer player,
            int sessionId,
            float reportedElapsedTicks,
            boolean pressed
    ) {
        ActiveSession active = ACTIVE.get(player.getUUID());
        if (active == null || active.snapshot.sessionId() != sessionId) {
            return;
        }
        if (!pressed) {
            resolve(player, active, TimingQteResult.MISSED_FAILURE);
            return;
        }

        float serverElapsed = active.snapshot.elapsedTicksAt(
                player.serverLevel().getGameTime(),
                0.0F
        );
        float earliestPlausible = serverElapsed - MAX_REPORTED_AGE_TICKS;
        float latestPlausible = serverElapsed + MAX_REPORTED_FUTURE_TICKS;
        float judgedElapsed = Float.isFinite(reportedElapsedTicks)
                ? Mth.clamp(reportedElapsedTicks, earliestPlausible, latestPlausible)
                : serverElapsed;
        resolve(player, active, active.snapshot.classifyPress(judgedElapsed));
    }

    public static void tick(ServerPlayer player) {
        ActiveSession active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return;
        }
        long automaticFailureTime = active.snapshot.cursorStartGameTime()
                + (long) Math.ceil(active.snapshot.successEndTick())
                + active.serverResponseGraceTicks;
        if (player.serverLevel().getGameTime() >= automaticFailureTime) {
            resolve(player, active, TimingQteResult.MISSED_FAILURE);
        }
    }

    public static void cancel(ServerPlayer player) {
        ActiveSession active = ACTIVE.remove(player.getUUID());
        if (active != null) {
            ModNetworking.sendTimingQteResolved(
                    player,
                    active.snapshot.sessionId(),
                    TimingQteResult.CANCELLED
            );
        }
    }

    public static void forgetPlayer(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    private static void resolve(ServerPlayer player, ActiveSession active, TimingQteResult result) {
        if (!ACTIVE.remove(player.getUUID(), active)) {
            return;
        }
        ModNetworking.sendTimingQteResolved(player, active.snapshot.sessionId(), result);
        active.resultHandler.handle(player, result);
    }

    private static int nextSessionId() {
        if (nextSessionId == Integer.MAX_VALUE) {
            nextSessionId = 1;
        }
        return nextSessionId++;
    }

    @FunctionalInterface
    public interface ResultHandler {
        void handle(ServerPlayer player, TimingQteResult result);
    }

    private record ActiveSession(
            TimingQteSnapshot snapshot,
            int serverResponseGraceTicks,
            ResultHandler resultHandler
    ) {
    }
}
