package com.swampd.superficialtrauma.client;

import net.minecraft.client.Minecraft;

public final class ClientGiveUpState {
    private static long endsGameTime = -1L;

    private ClientGiveUpState() {
    }

    public static void started(long endGameTime) {
        endsGameTime = Math.max(0L, endGameTime);
    }

    public static void clear() {
        endsGameTime = -1L;
    }

    public static boolean isActive() {
        return endsGameTime >= 0L;
    }

    public static float remainingSeconds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (endsGameTime < 0L || minecraft.level == null) {
            return 0.0F;
        }
        return Math.max(0L, endsGameTime - minecraft.level.getGameTime()) / 20.0F;
    }
}
