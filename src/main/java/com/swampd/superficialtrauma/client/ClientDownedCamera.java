package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientDownedCamera {
    private static final float MOTION_COMPLETION_PROGRESS = 0.9F;
    private static long activeDownedGameTime = Long.MIN_VALUE;
    private static float startingYaw;
    private static float startingPitch;

    private ClientDownedCamera() {
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            resetIfActive();
            return;
        }

        DownedPoseSnapshot snapshot = ClientDownedPoses.get(minecraft.player.getId()).orElse(null);
        if (snapshot == null) {
            resetIfActive();
            return;
        }
        if (snapshot.fallDirection() == DownedFallDirection.FADE_ONLY
                || snapshot.posture().usesFadeOnlyTransition()) {
            return;
        }

        if (activeDownedGameTime != snapshot.downedGameTime()) {
            activeDownedGameTime = snapshot.downedGameTime();
            startingYaw = event.getYaw();
            startingPitch = event.getPitch();
        }

        float transitionProgress = ClientDownedPoses.transitionProgress(minecraft.player.getId());
        float motionProgress = smootherStep(Mth.clamp(
                transitionProgress / MOTION_COMPLETION_PROGRESS,
                0.0F,
                1.0F
        ));
        CameraTarget target = targetFor(snapshot);
        event.setYaw(Mth.rotLerp(motionProgress, startingYaw, target.yaw()));
        event.setPitch(Mth.lerp(motionProgress, startingPitch, target.pitch()));
        event.setRoll(Mth.lerp(motionProgress, 0.0F, target.roll()));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && ClientDownedPoses.get(minecraft.player.getId()).isPresent()) {
            event.setCanceled(true);
        }
    }

    public static void reset() {
        activeDownedGameTime = Long.MIN_VALUE;
        startingYaw = 0.0F;
        startingPitch = 0.0F;
    }

    public static void onPoseChanged(int playerEntityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == playerEntityId) {
            reset();
        }
    }

    private static CameraTarget targetFor(DownedPoseSnapshot snapshot) {
        float yaw = snapshot.bodyYaw();
        return switch (snapshot.fallDirection()) {
            case FORWARD -> new CameraTarget(yaw, 78.0F, 0.0F);
            case BACKWARD -> new CameraTarget(yaw, -68.0F, 10.0F);
            case LEFT -> new CameraTarget(yaw - 10.0F, 24.0F, -78.0F);
            case RIGHT -> new CameraTarget(yaw + 10.0F, 24.0F, 78.0F);
            case FADE_ONLY -> new CameraTarget(yaw, startingPitch, 0.0F);
        };
    }

    private static float smootherStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0F - 15.0F) + 10.0F);
    }

    private static void resetIfActive() {
        if (activeDownedGameTime != Long.MIN_VALUE) {
            reset();
        }
    }

    private record CameraTarget(float yaw, float pitch, float roll) {
    }
}
