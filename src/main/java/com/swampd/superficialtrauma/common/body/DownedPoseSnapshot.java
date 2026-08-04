package com.swampd.superficialtrauma.common.body;

import net.minecraft.util.Mth;

public record DownedPoseSnapshot(
        long downedGameTime,
        float bodyYaw,
        DownedPosture posture,
        DownedFallDirection fallDirection
) {
    public DownedPoseSnapshot {
        downedGameTime = Math.max(0L, downedGameTime);
        bodyYaw = Mth.wrapDegrees(bodyYaw);
        posture = posture == null ? DownedPosture.UNSAFE : posture;
        fallDirection = fallDirection == null ? DownedFallDirection.FADE_ONLY : fallDirection;
        if (posture.usesFadeOnlyTransition()) {
            fallDirection = DownedFallDirection.FADE_ONLY;
        }
    }
}
