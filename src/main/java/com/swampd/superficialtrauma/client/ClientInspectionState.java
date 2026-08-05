package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class ClientInspectionState {
    private static int targetEntityId = -1;
    private static Component targetName = Component.empty();
    private static BodyState snapshot = new BodyState();
    private static float health;
    private static float maximumHealth;
    private static boolean received;

    private ClientInspectionState() {
    }

    public static void update(
            int entityId,
            Component name,
            float currentHealth,
            float currentMaximumHealth,
            CompoundTag bodyStateTag,
            boolean openScreen
    ) {
        BodyState updated = new BodyState();
        updated.deserializeNBT(bodyStateTag);
        targetEntityId = entityId;
        targetName = name.copy();
        snapshot = updated;
        health = currentHealth;
        maximumHealth = currentMaximumHealth;
        received = true;

        if (openScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof HealthScreen screen && screen.isInspecting(entityId))) {
                minecraft.setScreen(new HealthScreen(entityId, targetName));
            }
        }
    }

    public static void close(int entityId) {
        if (targetEntityId != entityId) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HealthScreen screen && screen.isInspecting(entityId)) {
            minecraft.setScreen(null);
        }
        clear();
    }

    public static int targetEntityId() {
        return targetEntityId;
    }

    public static Component targetName() {
        return targetName;
    }

    public static BodyState snapshot() {
        return snapshot;
    }

    public static float health() {
        return health;
    }

    public static float maximumHealth() {
        return maximumHealth;
    }

    public static boolean hasReceivedSnapshot(int entityId) {
        return received && targetEntityId == entityId;
    }

    public static void clear() {
        targetEntityId = -1;
        targetName = Component.empty();
        snapshot = new BodyState();
        health = 0.0F;
        maximumHealth = 0.0F;
        received = false;
    }
}
