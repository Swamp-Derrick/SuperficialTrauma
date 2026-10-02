package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientDownedPlayerRenderer {
    private static final float GROUND_CLEARANCE = 0.12F;
    private static final Deque<Integer> TRANSFORMED_PLAYERS = new ArrayDeque<>();

    private ClientDownedPlayerRenderer() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHideDeadPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.isDeadOrDying()) {
            return;
        }
        boolean hasReplacementCorpse = player.level()
                .getEntitiesOfClass(
                        CorpseEntity.class,
                        player.getBoundingBox().inflate(2.0D),
                        corpse -> corpse.ownerId().filter(player.getUUID()::equals).isPresent()
                )
                .stream()
                .findAny()
                .isPresent();
        if (hasReplacementCorpse) {
            event.setCanceled(true);
        }
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
            event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }

    private static void applyRigidGroundPose(
            PoseStack poseStack,
            Player player,
            DownedPoseSnapshot snapshot,
            float partialTick
    ) {
        float currentBodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float groundYaw = DownedGeometry.groundYaw(snapshot);
        float swimRotation = currentSwimRotation(player, partialTick);

        poseStack.translate(0.0F, GROUND_CLEARANCE, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - groundYaw));
        poseStack.translate(0.0F, 0.0F, -DownedGeometry.MODEL_CENTER_OFFSET);
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

    private static boolean isLocalInventoryPreview(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == player && minecraft.screen != null;
    }
}
