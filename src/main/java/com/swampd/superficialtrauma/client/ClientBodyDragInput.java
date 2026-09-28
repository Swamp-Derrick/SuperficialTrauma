package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public final class ClientBodyDragInput {
    private static final int KEEP_ALIVE_INTERVAL_TICKS = 5;
    private static int requestedTargetEntityId = -1;
    private static int keepAliveTicks;

    private ClientBodyDragInput() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear(false);
            return;
        }

        boolean draggerInWater = minecraft.player.isInWaterOrBubble();
        boolean blocked = minecraft.screen != null
                || ClientDownedPoses.get(minecraft.player.getId()).isPresent()
                || (!draggerInWater && minecraft.options.keyJump.isDown())
                || (!draggerInWater && !minecraft.options.keyShift.isDown());
        if (blocked || !ClientModEvents.DRAG_BODY.isDown()) {
            clear(true);
            return;
        }

        if (requestedTargetEntityId < 0) {
            Entity target = crosshairTarget(minecraft);
            if (!isDraggableClientTarget(target)) {
                return;
            }
            requestedTargetEntityId = target.getId();
            keepAliveTicks = 0;
            ModNetworking.setBodyDragHolding(requestedTargetEntityId, true);
            return;
        }

        keepAliveTicks++;
        if (keepAliveTicks >= KEEP_ALIVE_INTERVAL_TICKS) {
            keepAliveTicks = 0;
            ModNetworking.setBodyDragHolding(requestedTargetEntityId, true);
        }
    }

    public static void clear(boolean notifyServer) {
        if (requestedTargetEntityId >= 0 && notifyServer) {
            ModNetworking.setBodyDragHolding(requestedTargetEntityId, false);
        }
        requestedTargetEntityId = -1;
        keepAliveTicks = 0;
    }

    private static Entity crosshairTarget(Minecraft minecraft) {
        if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }
        return null;
    }

    private static boolean isDraggableClientTarget(Entity target) {
        if (target instanceof CorpseEntity) {
            return true;
        }
        return target instanceof Player player
                && ClientDownedPoses.get(player.getId()).isPresent();
    }
}
