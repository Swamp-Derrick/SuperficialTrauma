package com.swampd.superficialtrauma.common.drag;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative drag sessions shared by corpses and incapacitated players.
 * The session supplies only horizontal pull velocity; normal gravity and block
 * collision remain responsible for vertical movement and obstruction.
 */
public final class BodyDragService {
    private static final double START_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final double MAX_TETHER_DISTANCE_SQUARED = 3.25D * 3.25D;
    private static final double SLACK_DISTANCE = 1.2D;
    private static final double SPRING_STRENGTH = 0.18D;
    private static final double MAX_PULL_SPEED = 0.18D;
    private static final long KEEP_ALIVE_TIMEOUT_TICKS = 15L;
    private static final Map<UUID, DragSession> SESSIONS_BY_DRAGGER = new HashMap<>();
    private static final Map<UUID, UUID> DRAGGER_BY_TARGET = new HashMap<>();

    private BodyDragService() {
    }

    public static void setHolding(ServerPlayer dragger, int targetEntityId, boolean holding) {
        if (!holding) {
            stop(dragger.getUUID());
            return;
        }

        DragSession existing = SESSIONS_BY_DRAGGER.get(dragger.getUUID());
        if (existing != null) {
            Entity target = findTarget(dragger, existing);
            if (target != null && target.getId() == targetEntityId) {
                existing.lastKeepAliveGameTime = dragger.serverLevel().getGameTime();
                return;
            }
            stop(dragger.getUUID());
        }

        Entity target = dragger.serverLevel().getEntity(targetEntityId);
        if (!canStart(dragger, target)) {
            return;
        }
        UUID targetId = target.getUUID();
        UUID occupyingDragger = DRAGGER_BY_TARGET.get(targetId);
        if (occupyingDragger != null && !occupyingDragger.equals(dragger.getUUID())) {
            return;
        }

        DragSession session = new DragSession(
                targetId,
                target instanceof CorpseEntity,
                dragger.serverLevel().getGameTime(),
                dragger.getId(),
                target.getId(),
                dragger.getServer()
        );
        SESSIONS_BY_DRAGGER.put(dragger.getUUID(), session);
        DRAGGER_BY_TARGET.put(targetId, dragger.getUUID());
        applyDraggerState(dragger);
        ModNetworking.syncBodyDragState(target, dragger, true);
    }

    public static void tick(ServerPlayer player) {
        DragSession session = SESSIONS_BY_DRAGGER.get(player.getUUID());
        if (session == null) {
            return;
        }

        Entity target = findTarget(player, session);
        long gameTime = player.serverLevel().getGameTime();
        if (gameTime - session.lastKeepAliveGameTime > KEEP_ALIVE_TIMEOUT_TICKS
                || !canContinue(player, target, session)) {
            stop(player.getUUID());
            return;
        }
        applyDraggerState(player);
    }

    public static Vec3 horizontalPull(Entity target) {
        UUID draggerId = DRAGGER_BY_TARGET.get(target.getUUID());
        if (draggerId == null || target.getServer() == null) {
            return Vec3.ZERO;
        }
        ServerPlayer dragger = target.getServer().getPlayerList().getPlayer(draggerId);
        DragSession session = SESSIONS_BY_DRAGGER.get(draggerId);
        if (dragger == null || session == null || !canContinue(dragger, target, session)) {
            stop(draggerId);
            return Vec3.ZERO;
        }

        double x = dragger.getX() - target.getX();
        double z = dragger.getZ() - target.getZ();
        double distance = Math.sqrt(x * x + z * z);
        if (distance <= SLACK_DISTANCE || distance < 1.0E-5D) {
            return Vec3.ZERO;
        }
        double speed = Math.min(MAX_PULL_SPEED, (distance - SLACK_DISTANCE) * SPRING_STRENGTH);
        return new Vec3(x / distance * speed, 0.0D, z / distance * speed);
    }

    public static boolean isBeingDragged(Entity target) {
        return DRAGGER_BY_TARGET.containsKey(target.getUUID());
    }

    public static void syncForTracking(Entity target, ServerPlayer receiver) {
        UUID draggerId = DRAGGER_BY_TARGET.get(target.getUUID());
        if (draggerId == null || target.getServer() == null) {
            return;
        }
        ServerPlayer dragger = target.getServer().getPlayerList().getPlayer(draggerId);
        if (dragger != null) {
            ModNetworking.syncBodyDragStateTo(target, dragger, receiver, true);
        }
    }

    public static void forgetPlayer(UUID playerId) {
        stop(playerId);
        UUID draggerId = DRAGGER_BY_TARGET.get(playerId);
        if (draggerId != null) {
            stop(draggerId);
        }
    }

    public static void clearAll() {
        for (UUID draggerId : SESSIONS_BY_DRAGGER.keySet().toArray(UUID[]::new)) {
            stop(draggerId);
        }
        SESSIONS_BY_DRAGGER.clear();
        DRAGGER_BY_TARGET.clear();
    }

    private static boolean canStart(ServerPlayer dragger, Entity target) {
        return canDrag(dragger)
                && dragger.isShiftKeyDown()
                && target != null
                && target != dragger
                && target.level() == dragger.level()
                && dragger.distanceToSqr(target) <= START_DISTANCE_SQUARED
                && dragger.hasLineOfSight(target)
                && isDraggableTarget(target);
    }

    private static boolean canContinue(ServerPlayer dragger, Entity target, DragSession session) {
        if (!canDrag(dragger)
                || !dragger.isShiftKeyDown()
                || target == null
                || target.isRemoved()
                || target.level() != dragger.level()
                || dragger.distanceToSqr(target) > MAX_TETHER_DISTANCE_SQUARED
                || session.corpse != (target instanceof CorpseEntity)) {
            return false;
        }
        return isDraggableTarget(target);
    }

    private static boolean canDrag(ServerPlayer dragger) {
        if (!dragger.isAlive() || dragger.isRemoved() || dragger.containerMenu != dragger.inventoryMenu) {
            return false;
        }
        return BodyStateCapability.get(dragger)
                .map(bodyState -> bodyState.canAct())
                .orElse(true);
    }

    private static boolean isDraggableTarget(Entity target) {
        if (target instanceof CorpseEntity corpse) {
            return !corpse.isRemoved();
        }
        if (!(target instanceof ServerPlayer player) || !player.isAlive()) {
            return false;
        }
        return BodyStateCapability.get(player)
                .map(bodyState -> !bodyState.canAct())
                .orElse(false);
    }

    private static Entity findTarget(ServerPlayer dragger, DragSession session) {
        ServerLevel level = dragger.serverLevel();
        if (session.corpse) {
            return level.getEntity(session.targetId);
        }
        return dragger.getServer().getPlayerList().getPlayer(session.targetId);
    }

    private static void applyDraggerState(ServerPlayer dragger) {
        dragger.setSprinting(false);
    }

    private static void stop(UUID draggerId) {
        DragSession session = SESSIONS_BY_DRAGGER.remove(draggerId);
        if (session == null) {
            return;
        }
        DRAGGER_BY_TARGET.remove(session.targetId, draggerId);

        ServerPlayer dragger = session.server.getPlayerList().getPlayer(draggerId);
        Entity target = findTarget(session);
        if (target != null && !target.isRemoved()) {
            Vec3 movement = target.getDeltaMovement();
            target.setDeltaMovement(0.0D, movement.y, 0.0D);
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.hurtMarked = true;
            }
        }
        ModNetworking.syncBodyDragState(
                session.targetEntityId,
                session.draggerEntityId,
                false
        );
    }

    private static Entity findTarget(DragSession session) {
        if (!session.corpse) {
            return session.server.getPlayerList().getPlayer(session.targetId);
        }
        for (ServerLevel level : session.server.getAllLevels()) {
            Entity target = level.getEntity(session.targetId);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private static final class DragSession {
        private final UUID targetId;
        private final boolean corpse;
        private long lastKeepAliveGameTime;
        private final int draggerEntityId;
        private final int targetEntityId;
        private final net.minecraft.server.MinecraftServer server;

        private DragSession(
                UUID targetId,
                boolean corpse,
                long lastKeepAliveGameTime,
                int draggerEntityId,
                int targetEntityId,
                net.minecraft.server.MinecraftServer server
        ) {
            this.targetId = targetId;
            this.corpse = corpse;
            this.lastKeepAliveGameTime = lastKeepAliveGameTime;
            this.draggerEntityId = draggerEntityId;
            this.targetEntityId = targetEntityId;
            this.server = server;
        }
    }
}
