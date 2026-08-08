package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.GiveUpService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ClientGiveUpInput {
    private static final int HEARTBEAT_INTERVAL_TICKS = 5;
    private static boolean holdingSent;
    private static int heartbeatTicks;

    private ClientGiveUpInput() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean eligible = minecraft.player != null
                && minecraft.getConnection() != null
                && ClientDownedOverlay.isVisible()
                && GiveUpService.isEligible(ClientBodyState.snapshot());
        boolean spaceHeld = eligible
                && GLFW.glfwGetKey(
                minecraft.getWindow().getWindow(),
                GLFW.GLFW_KEY_SPACE
        ) == GLFW.GLFW_PRESS;

        if (!spaceHeld) {
            if (holdingSent) {
                ModNetworking.setGiveUpHolding(false);
            }
            holdingSent = false;
            heartbeatTicks = 0;
            return;
        }

        heartbeatTicks++;
        if (!holdingSent || heartbeatTicks >= HEARTBEAT_INTERVAL_TICKS) {
            ModNetworking.setGiveUpHolding(true);
            holdingSent = true;
            heartbeatTicks = 0;
        }
    }

    public static void clear() {
        holdingSent = false;
        heartbeatTicks = 0;
        ClientGiveUpState.clear();
    }
}
