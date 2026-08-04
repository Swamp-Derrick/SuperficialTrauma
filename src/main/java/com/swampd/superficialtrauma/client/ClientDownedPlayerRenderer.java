package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientDownedPlayerRenderer {
    private static final float GROUND_CLEARANCE = 0.12F;
    private static final Deque<Integer> TRANSFORMED_PLAYERS = new ArrayDeque<>();

    private ClientDownedPlayerRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        DownedPoseSnapshot snapshot = ClientDownedPoses.get(player.getId()).orElse(null);
        if (snapshot == null
                || snapshot.posture() == DownedPosture.UNSAFE
                || isLocalInventoryPreview(player)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyRigidGroundPose(poseStack, player, snapshot, event.getPartialTick());
        TRANSFORMED_PLAYERS.push(player.getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!TRANSFORMED_PLAYERS.isEmpty()
                && TRANSFORMED_PLAYERS.peek() == event.getEntity().getId()) {
            TRANSFORMED_PLAYERS.pop();
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player player
                && ClientDownedPoses.get(player.getId()).isPresent()) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static void applyRigidGroundPose(
            PoseStack poseStack,
            Player player,
            DownedPoseSnapshot snapshot,
            float partialTick
    ) {
        float currentBodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float groundYaw = Mth.wrapDegrees(
                snapshot.bodyYaw() + fallDirectionYawOffset(snapshot.fallDirection())
        );
        float swimRotation = currentSwimRotation(player, partialTick);

        poseStack.translate(0.0F, GROUND_CLEARANCE, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - groundYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (player.isVisuallySwimming()) {
            poseStack.translate(0.0F, 1.0F, -0.3F);
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(-swimRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(currentBodyYaw - 180.0F));
    }

    private static float currentSwimRotation(Player player, float partialTick) {
        float swimAmount = player.getSwimAmount(partialTick);
        if (swimAmount <= 0.0F) {
            return 0.0F;
        }
        boolean swimmingInFluid = player.isInWater()
                || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType));
        float targetRotation = swimmingInFluid ? -90.0F - player.getXRot() : -90.0F;
        return Mth.lerp(swimAmount, 0.0F, targetRotation);
    }

    private static float fallDirectionYawOffset(DownedFallDirection direction) {
        return switch (direction) {
            case FORWARD, FADE_ONLY -> 0.0F;
            case BACKWARD -> 180.0F;
            case LEFT -> -90.0F;
            case RIGHT -> 90.0F;
        };
    }

    private static boolean isLocalInventoryPreview(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == player && minecraft.screen != null;
    }
}
