package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.nbt.CompoundTag;

public final class ClientBodyState {
    private static BodyState snapshot = new BodyState();
    private static boolean received;

    private ClientBodyState() {
    }

    public static BodyState snapshot() {
        return snapshot;
    }

    public static boolean hasReceivedSnapshot() {
        return received;
    }

    public static void update(CompoundTag tag) {
        BodyState updated = new BodyState();
        updated.deserializeNBT(tag);
        snapshot = updated;
        received = true;
        ClientAwakeningRecovery.synchronize(updated);
    }

    public static void clear() {
        snapshot = new BodyState();
        received = false;
        ClientAwakeningRecovery.clear();
    }
}
