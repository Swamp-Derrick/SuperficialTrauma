package com.swampd.superficialtrauma.common.drag;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.DownedHitbox;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative hold action for rotating incapacitated bodies in place. */
public final class BodyRotationService {
    private static final double START_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final double MAX_DISTANCE_SQUARED = 3.25D * 3.25D;
    private static final float CLOCKWISE_DEGREES_PER_TICK = 1.5F;
    private static final long KEEP_ALIVE_TIMEOUT_TICKS = 15L;
    private static final Map<UUID, RotationSession> SESSIONS_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_TARGET = new HashMap<>();

    private BodyRotationService() {
    }

    public static void setHolding(ServerPlayer actor, int targetEntityId, boolean holding) {
        if (!holding) {
            stop(actor.getUUID());
            return;
        }

        RotationSession existing = SESSIONS_BY_ACTOR.get(actor.getUUID());
        if (existing != null) {
            Entity existingTarget = findTarget(actor, existing);
            if (existingTarget != null && existingTarget.getId() == targetEntityId) {
                existing.lastKeepAliveGameTime = actor.serverLevel().getGameTime();
                return;
            }
            stop(actor.getUUID());
        }

        Entity target = actor.serverLevel().getEntity(targetEntityId);
        if (!canStart(actor, target)) {
            return;
        }
        UUID occupyingActor = ACTOR_BY_TARGET.get(target.getUUID());
        if (occupyingActor != null && !occupyingActor.equals(actor.getUUID())) {
            return;
        }

        RotationSession session = new RotationSession(
                target.getUUID(),
                target instanceof CorpseEntity,
                actor.serverLevel().getGameTime()
        );
        SESSIONS_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_TARGET.put(target.getUUID(), actor.getUUID());
    }

    public static void tick(ServerPlayer actor) {
        RotationSession session = SESSIONS_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        Entity target = findTarget(actor, session);
        long gameTime = actor.serverLevel().getGameTime();
        if (gameTime - session.lastKeepAliveGameTime > KEEP_ALIVE_TIMEOUT_TICKS
                || !canContinue(actor, target, session)
                || !rotateTarget(target)) {
            stop(actor.getUUID());
        }
    }

    public static void forgetPlayer(UUID playerId) {
        stop(playerId);
        UUID actorId = ACTOR_BY_TARGET.get(playerId);
        if (actorId != null) {
            stop(actorId);
        }
    }

    public static void clearAll() {
        SESSIONS_BY_ACTOR.clear();
        ACTOR_BY_TARGET.clear();
    }

    private static boolean canStart(ServerPlayer actor, Entity target) {
        return canOperate(actor)
                && actor.isShiftKeyDown()
                && target != null
                && target != actor
                && target.level() == actor.level()
                && actor.distanceToSqr(target) <= START_DISTANCE_SQUARED
                && actor.hasLineOfSight(target)
                && isRotatableTarget(target);
    }

    private static boolean canContinue(ServerPlayer actor, Entity target, RotationSession session) {
        return canOperate(actor)
                && actor.isShiftKeyDown()
                && target != null
                && !target.isRemoved()
                && target.level() == actor.level()
                && actor.distanceToSqr(target) <= MAX_DISTANCE_SQUARED
                && session.corpse == (target instanceof CorpseEntity)
                && isRotatableTarget(target);
    }

    private static boolean canOperate(ServerPlayer actor) {
        if (!actor.isAlive() || actor.isRemoved() || actor.containerMenu != actor.inventoryMenu) {
            return false;
        }
        return BodyStateCapability.get(actor).map(bodyState -> bodyState.canAct()).orElse(true);
    }

    private static boolean isRotatableTarget(Entity target) {
        if (target instanceof CorpseEntity corpse) {
            return !corpse.isRemoved();
        }
        if (!(target instanceof ServerPlayer player) || !player.isAlive()) {
            return false;
        }
        return BodyStateCapability.get(player).map(bodyState -> !bodyState.canAct()).orElse(false);
    }

    private static boolean rotateTarget(Entity target) {
        if (target instanceof CorpseEntity corpse) {
            corpse.rotateBody(CLOCKWISE_DEGREES_PER_TICK);
            return true;
        }
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        return BodyStateCapability.get(player).map(bodyState -> {
            if (!bodyState.rotateDownedPose(CLOCKWISE_DEGREES_PER_TICK)) {
                return false;
            }
            DownedHitbox.update(player, bodyState);
            ModNetworking.syncDownedPose(player);
            return true;
        }).orElse(false);
    }

    private static Entity findTarget(ServerPlayer actor, RotationSession session) {
        ServerLevel level = actor.serverLevel();
        if (session.corpse) {
            return level.getEntity(session.targetId);
        }
        return actor.getServer().getPlayerList().getPlayer(session.targetId);
    }

    private static void stop(UUID actorId) {
        RotationSession session = SESSIONS_BY_ACTOR.remove(actorId);
        if (session != null) {
            ACTOR_BY_TARGET.remove(session.targetId, actorId);
        }
    }

    private static final class RotationSession {
        private final UUID targetId;
        private final boolean corpse;
        private long lastKeepAliveGameTime;

        private RotationSession(UUID targetId, boolean corpse, long lastKeepAliveGameTime) {
            this.targetId = targetId;
            this.corpse = corpse;
            this.lastKeepAliveGameTime = lastKeepAliveGameTime;
        }
    }
}
