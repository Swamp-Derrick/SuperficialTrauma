package com.swampd.superficialtrauma.common.body;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class DownedPoseCapture {
    private static final double MIN_HORIZONTAL_LENGTH_SQUARED = 1.0E-4D;

    private DownedPoseCapture() {
    }

    public static DownedPoseSnapshot capture(
            ServerPlayer player,
            DamageSource damageSource,
            long gameTime
    ) {
        DownedPosture posture = classifyPosture(player);
        float bodyYaw = Mth.wrapDegrees(player.getYRot());
        DownedFallDirection fallDirection = posture.usesFadeOnlyTransition()
                ? DownedFallDirection.FADE_ONLY
                : classifyFallDirection(bodyYaw, chooseFallVector(player, damageSource, posture));
        return new DownedPoseSnapshot(gameTime, bodyYaw, posture, fallDirection);
    }

    static DownedPosture classifyPosture(ServerPlayer player) {
        boolean unsafe = player.isPassenger()
                || player.isFallFlying()
                || player.onClimbable()
                || player.isSleeping();
        return classifyPosture(
                unsafe,
                player.isVisuallyCrawling(),
                player.isVisuallySwimming(),
                player.isSprinting(),
                player.isCrouching()
        );
    }

    static DownedPosture classifyPosture(
            boolean unsafe,
            boolean visuallyCrawling,
            boolean visuallySwimming,
            boolean sprinting,
            boolean crouching
    ) {
        if (unsafe) {
            return DownedPosture.UNSAFE;
        }
        if (visuallyCrawling) {
            return DownedPosture.CRAWLING;
        }
        if (visuallySwimming) {
            return DownedPosture.SWIMMING;
        }
        if (sprinting) {
            return DownedPosture.SPRINTING;
        }
        if (crouching) {
            return DownedPosture.CROUCHING;
        }
        return DownedPosture.STANDING;
    }

    static DownedFallDirection classifyFallDirection(float bodyYaw, Vec3 worldFallVector) {
        if (!hasHorizontalDirection(worldFallVector)) {
            return DownedFallDirection.FADE_ONLY;
        }

        float vectorYaw = (float) Math.toDegrees(Math.atan2(-worldFallVector.x, worldFallVector.z));
        float relativeYaw = Mth.wrapDegrees(vectorYaw - bodyYaw);
        float absoluteYaw = Math.abs(relativeYaw);
        if (absoluteYaw <= 45.0F) {
            return DownedFallDirection.FORWARD;
        }
        if (absoluteYaw >= 135.0F) {
            return DownedFallDirection.BACKWARD;
        }
        return relativeYaw > 0.0F ? DownedFallDirection.RIGHT : DownedFallDirection.LEFT;
    }

    private static Vec3 chooseFallVector(
            ServerPlayer player,
            DamageSource damageSource,
            DownedPosture posture
    ) {
        if (posture == DownedPosture.SPRINTING && hasHorizontalDirection(player.getDeltaMovement())) {
            return player.getDeltaMovement();
        }
        if (damageSource == null) {
            return Vec3.ZERO;
        }

        Entity directEntity = damageSource.getDirectEntity();
        Entity causingEntity = damageSource.getEntity();
        if (directEntity != null
                && directEntity != causingEntity
                && hasHorizontalDirection(directEntity.getDeltaMovement())) {
            return directEntity.getDeltaMovement();
        }

        Vec3 sourcePosition = damageSource.getSourcePosition();
        if (sourcePosition != null) {
            Vec3 awayFromSource = player.position().subtract(sourcePosition);
            if (hasHorizontalDirection(awayFromSource)) {
                return awayFromSource;
            }
        }
        return Vec3.ZERO;
    }

    private static boolean hasHorizontalDirection(Vec3 vector) {
        return vector != null
                && vector.x * vector.x + vector.z * vector.z >= MIN_HORIZONTAL_LENGTH_SQUARED;
    }
}
