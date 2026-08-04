package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientDownedPoses {
    private static final long FADE_DURATION_NANOS = 900_000_000L;
    private static final Map<Integer, DownedPoseSnapshot> SNAPSHOTS = new HashMap<>();
    private static final Map<Integer, Long> TRANSITION_STARTED_NANOS = new HashMap<>();

    private ClientDownedPoses() {
    }

    public static Optional<DownedPoseSnapshot> get(int playerEntityId) {
        return Optional.ofNullable(SNAPSHOTS.get(playerEntityId));
    }

    public static void update(int playerEntityId, DownedPoseSnapshot snapshot) {
        boolean transitionChanged;
        if (snapshot == null) {
            transitionChanged = SNAPSHOTS.remove(playerEntityId) != null;
            TRANSITION_STARTED_NANOS.remove(playerEntityId);
        } else {
            DownedPoseSnapshot previous = SNAPSHOTS.put(playerEntityId, snapshot);
            transitionChanged = previous == null
                    || previous.downedGameTime() != snapshot.downedGameTime();
            if (transitionChanged) {
                TRANSITION_STARTED_NANOS.put(playerEntityId, System.nanoTime());
            }
        }
        ClientDownedHitbox.onPoseChanged(playerEntityId);
        ClientDownedInput.onPoseChanged(playerEntityId);
        if (transitionChanged) {
            ClientDownedCamera.onPoseChanged(playerEntityId);
        }
    }

    public static float transitionProgress(int playerEntityId) {
        if (!SNAPSHOTS.containsKey(playerEntityId)) {
            return 0.0F;
        }
        Long startedNanos = TRANSITION_STARTED_NANOS.get(playerEntityId);
        if (startedNanos == null) {
            return 1.0F;
        }
        long elapsedNanos = Math.max(0L, System.nanoTime() - startedNanos);
        return Math.min(1.0F, elapsedNanos / (float) FADE_DURATION_NANOS);
    }

    public static void clear() {
        SNAPSHOTS.clear();
        TRANSITION_STARTED_NANOS.clear();
        ClientDownedCamera.reset();
    }
}
