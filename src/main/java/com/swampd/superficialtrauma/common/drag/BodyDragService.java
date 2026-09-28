package com.swampd.superficialtrauma.common.drag;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative drag sessions shared by corpses and incapacitated players.
 * Normal gravity and block collision remain responsible for obstruction. A
 * small vertical pull is supplied only while actively hauling a body onto the
 * top of a full one-block obstacle.
 */
public final class BodyDragService {
    private static final double START_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final double MAX_TETHER_DISTANCE_SQUARED = 3.25D * 3.25D;
    private static final double SLACK_DISTANCE = 1.2D;
    private static final double SPRING_STRENGTH = 0.18D;
    private static final double MAX_PULL_SPEED = 0.18D;
    private static final double FULL_BLOCK_CLIMB_PULL_SPEED = 0.025D;
    private static final double FULL_BLOCK_CLIMB_VERTICAL_SPEED = 0.055D;
    private static final double FULL_BLOCK_TOP_HOLD_SPEED = 0.001D;
    private static final double MIN_REMAINING_CLIMB_HEIGHT = 0.001D;
    private static final double MAX_CLIMB_HEIGHT = 1.05D;
    private static final double CLIMB_PROBE_DISTANCE = 0.20D;
    private static final double MIN_PLATFORM_CONTACT_AREA = 0.002D;
    private static final double MIN_TOP_SUPPORT_AREA = 0.06D;
    private static final double TARGET_WATER_DEPTH = 0.30D;
    private static final double WATER_DEPTH_RESPONSE = 0.18D;
    private static final double MAX_WATER_RISE_SPEED = 0.06D;
    private static final double MAX_WATER_SINK_SPEED = 0.03D;
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

    public static Vec3 pullMovement(Entity target) {
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
        if (distance < 1.0E-5D) {
            return Vec3.ZERO;
        }
        double directionX = x / distance;
        double directionZ = z / distance;
        Integer climbSurfaceY = fullBlockClimbSurfaceY(
                target,
                dragger,
                session,
                directionX,
                directionZ
        );
        if (climbSurfaceY != null) {
            double verticalSpeed = target.getBoundingBox().minY < climbSurfaceY
                    ? FULL_BLOCK_CLIMB_VERTICAL_SPEED
                    : FULL_BLOCK_TOP_HOLD_SPEED;
            return new Vec3(
                    directionX * FULL_BLOCK_CLIMB_PULL_SPEED,
                    verticalSpeed,
                    directionZ * FULL_BLOCK_CLIMB_PULL_SPEED
            );
        }
        if (distance <= SLACK_DISTANCE) {
            return Vec3.ZERO;
        }
        double speed = Math.min(MAX_PULL_SPEED, (distance - SLACK_DISTANCE) * SPRING_STRENGTH);
        return new Vec3(directionX * speed, 0.0D, directionZ * speed);
    }

    public static double bodyVerticalMovement(Entity target, double currentVerticalMovement, double dragLift) {
        double verticalMovement;
        if (target.isInWaterOrBubble()) {
            double waterDepthError = target.getFluidHeight(FluidTags.WATER) - TARGET_WATER_DEPTH;
            verticalMovement = Mth.clamp(
                    waterDepthError * WATER_DEPTH_RESPONSE,
                    -MAX_WATER_SINK_SPEED,
                    MAX_WATER_RISE_SPEED
            );
        } else {
            verticalMovement = Math.min(0.0D, currentVerticalMovement);
        }
        return dragLift > 0.0D
                ? Math.max(verticalMovement, dragLift)
                : verticalMovement;
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
                && hasRequiredDragPosture(dragger)
                && target != null
                && target != dragger
                && target.level() == dragger.level()
                && dragger.distanceToSqr(target) <= START_DISTANCE_SQUARED
                && dragger.hasLineOfSight(target)
                && isDraggableTarget(target);
    }

    private static boolean canContinue(ServerPlayer dragger, Entity target, DragSession session) {
        if (!canDrag(dragger)
                || !hasRequiredDragPosture(dragger)
                || target == null
                || target.isRemoved()
                || target.level() != dragger.level()
                || dragger.distanceToSqr(target) > MAX_TETHER_DISTANCE_SQUARED
                || session.corpse != (target instanceof CorpseEntity)) {
            return false;
        }
        return isDraggableTarget(target);
    }

    private static boolean hasRequiredDragPosture(ServerPlayer dragger) {
        return dragger.isShiftKeyDown() || dragger.isInWaterOrBubble();
    }

    private static Integer fullBlockClimbSurfaceY(
            Entity target,
            ServerPlayer dragger,
            DragSession session,
            double directionX,
            double directionZ
    ) {
        if (session.climbSurfaceY != null) {
            if (canContinueFullBlockClimb(
                    target,
                    dragger,
                    session.climbSurfaceY,
                    directionX,
                    directionZ
            )) {
                return session.climbSurfaceY;
            }
            session.climbSurfaceY = null;
        }

        AABB targetBounds = target.getBoundingBox();
        int obstacleY = Mth.floor(targetBounds.minY + 1.0E-4D);
        int surfaceY = obstacleY + 1;
        double climbHeight = surfaceY - targetBounds.minY;
        if (climbHeight < MIN_REMAINING_CLIMB_HEIGHT || climbHeight > MAX_CLIMB_HEIGHT
                || dragger.getY() < surfaceY - 0.25D) {
            return null;
        }

        AABB sweptBounds = horizontalSweep(targetBounds, directionX, directionZ);
        if (fullBlockOverlapArea(target, sweptBounds, obstacleY) < MIN_PLATFORM_CONTACT_AREA) {
            return null;
        }

        AABB raisedBounds = targetBounds.move(0.0D, climbHeight + 0.01D, 0.0D);
        if (!target.level().noCollision(target, raisedBounds)) {
            return null;
        }
        session.climbSurfaceY = surfaceY;
        return session.climbSurfaceY;
    }

    private static boolean canContinueFullBlockClimb(
            Entity target,
            ServerPlayer dragger,
            int surfaceY,
            double directionX,
            double directionZ
    ) {
        AABB targetBounds = target.getBoundingBox();
        double remainingClimb = surfaceY - targetBounds.minY;
        if (remainingClimb > MAX_CLIMB_HEIGHT
                || remainingClimb < -0.20D
                || dragger.getY() < surfaceY - 0.25D) {
            return false;
        }
        int obstacleY = surfaceY - 1;
        AABB sweptBounds = horizontalSweep(targetBounds, directionX, directionZ);
        if (fullBlockOverlapArea(target, sweptBounds, obstacleY) < MIN_PLATFORM_CONTACT_AREA) {
            return false;
        }
        return targetBounds.minY < surfaceY - 0.01D
                || fullBlockOverlapArea(target, targetBounds, obstacleY) < MIN_TOP_SUPPORT_AREA;
    }

    private static AABB horizontalSweep(AABB targetBounds, double directionX, double directionZ) {
        return targetBounds.expandTowards(
                directionX * CLIMB_PROBE_DISTANCE,
                0.0D,
                directionZ * CLIMB_PROBE_DISTANCE
        );
    }

    private static double fullBlockOverlapArea(Entity target, AABB bounds, int blockY) {
        int minimumX = Mth.floor(bounds.minX + 1.0E-7D);
        int maximumX = Mth.floor(bounds.maxX - 1.0E-7D);
        int minimumZ = Mth.floor(bounds.minZ + 1.0E-7D);
        int maximumZ = Mth.floor(bounds.maxZ - 1.0E-7D);
        double totalArea = 0.0D;

        for (int x = minimumX; x <= maximumX; x++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                BlockPos blockPos = new BlockPos(x, blockY, z);
                BlockState blockState = target.level().getBlockState(blockPos);
                if (!blockState.isCollisionShapeFullBlock(target.level(), blockPos)) {
                    continue;
                }
                double overlapX = Math.max(0.0D, Math.min(bounds.maxX, x + 1.0D) - Math.max(bounds.minX, x));
                double overlapZ = Math.max(0.0D, Math.min(bounds.maxZ, z + 1.0D) - Math.max(bounds.minZ, z));
                totalArea += overlapX * overlapZ;
            }
        }
        return totalArea;
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
        private Integer climbSurfaceY;

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
