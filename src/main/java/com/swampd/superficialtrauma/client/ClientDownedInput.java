package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientDownedInput {
    private ClientDownedInput() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || !isLocalPlayerDowned()) {
            return;
        }

        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (isLocalPlayerDowned() && isBlockedScreen(event.getNewScreen())) {
            event.setCanceled(true);
        }
    }

    public static void suppressKeyActions() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isLocalPlayerDowned()) {
            return;
        }

        consumeAndRelease(minecraft.options.keyInventory);
        consumeAndRelease(minecraft.options.keyChat);
        consumeAndRelease(minecraft.options.keyCommand);
        consumeAndRelease(minecraft.options.keyDrop);
        consumeAndRelease(minecraft.options.keySwapOffhand);
        release(minecraft.options.keyUp);
        release(minecraft.options.keyDown);
        release(minecraft.options.keyLeft);
        release(minecraft.options.keyRight);
        release(minecraft.options.keyJump);
        release(minecraft.options.keyShift);
        release(minecraft.options.keySprint);
    }

    public static void enforceMovementLock() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isLocalPlayerDowned()) {
            return;
        }
        minecraft.player.setSprinting(false);
        minecraft.player.setShiftKeyDown(false);
        Vec3 movement = minecraft.player.getDeltaMovement();
        if (ClientBodyDragState.isBeingDragged(minecraft.player.getId())) {
            minecraft.player.setDeltaMovement(
                    movement.x,
                    Math.min(0.0D, movement.y),
                    movement.z
            );
        } else {
            minecraft.player.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
        }
    }

    public static void onPoseChanged(int playerEntityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.getId() != playerEntityId) {
            return;
        }
        if (ClientDownedPoses.get(playerEntityId).isPresent()
                && isBlockedScreen(minecraft.screen)) {
            minecraft.setScreen(null);
        }
    }

    private static boolean isLocalPlayerDowned() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && ClientDownedPoses.get(minecraft.player.getId()).isPresent();
    }

    private static boolean isBlockedScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> || screen instanceof ChatScreen;
    }

    private static void consumeAndRelease(KeyMapping keyMapping) {
        while (keyMapping.consumeClick()) {
            // Drain queued presses before vanilla handles them.
        }
        release(keyMapping);
    }

    private static void release(KeyMapping keyMapping) {
        keyMapping.setDown(false);
    }
}
