package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientDownedPoses {
    private static final Map<Integer, DownedPoseSnapshot> SNAPSHOTS = new HashMap<>();

    private ClientDownedPoses() {
    }

    public static Optional<DownedPoseSnapshot> get(int playerEntityId) {
        return Optional.ofNullable(SNAPSHOTS.get(playerEntityId));
    }

    public static void update(int playerEntityId, DownedPoseSnapshot snapshot) {
        if (snapshot == null) {
            SNAPSHOTS.remove(playerEntityId);
        } else {
            SNAPSHOTS.put(playerEntityId, snapshot);
        }
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }
}
