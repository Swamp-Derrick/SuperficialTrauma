package com.swampd.superficialtrauma.common.body;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

public final class DownedGeometry {
    public static final float BODY_LENGTH = 1.8F;
    public static final float BODY_WIDTH = 0.6F;
    public static final float BODY_HEIGHT = 0.6F;
    public static final float EYE_HEIGHT = 0.3F;
    public static final float MODEL_CENTER_OFFSET = BODY_LENGTH / 2.0F;
    public static final EntityDimensions ENTITY_DIMENSIONS = EntityDimensions.scalable(
            BODY_WIDTH,
            BODY_HEIGHT
    ).withEyeHeight(EYE_HEIGHT);

    private DownedGeometry() {
    }

    public static float groundYaw(DownedPoseSnapshot snapshot) {
        return Mth.wrapDegrees(
                snapshot.bodyYaw() + fallDirectionYawOffset(snapshot.fallDirection())
        );
    }

    public static AABB boundingBox(Entity entity, DownedPoseSnapshot snapshot) {
        Vec2 halfExtents = horizontalHalfExtents(groundYaw(snapshot));
        return new AABB(
                entity.getX() - halfExtents.x,
                entity.getY(),
                entity.getZ() - halfExtents.y,
                entity.getX() + halfExtents.x,
                entity.getY() + BODY_HEIGHT,
                entity.getZ() + halfExtents.y
        );
    }

    static Vec2 horizontalHalfExtents(float groundYaw) {
        double radians = Math.toRadians(groundYaw);
        float absoluteSin = (float) Math.abs(Math.sin(radians));
        float absoluteCos = (float) Math.abs(Math.cos(radians));
        float halfLength = BODY_LENGTH / 2.0F;
        float halfWidth = BODY_WIDTH / 2.0F;
        return new Vec2(
                absoluteSin * halfLength + absoluteCos * halfWidth,
                absoluteCos * halfLength + absoluteSin * halfWidth
        );
    }

    private static float fallDirectionYawOffset(DownedFallDirection direction) {
        return switch (direction) {
            case FORWARD, FADE_ONLY -> 0.0F;
            case BACKWARD -> 180.0F;
            case LEFT -> -90.0F;
            case RIGHT -> 90.0F;
        };
    }
}
