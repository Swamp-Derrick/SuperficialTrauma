package com.swampd.superficialtrauma.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class CorpseRenderer extends LivingEntityRenderer<CorpseEntity, PlayerModel<CorpseEntity>> {
    private static final float GROUND_CLEARANCE = 0.12F;
    private final PlayerModel<CorpseEntity> wideModel;
    private final PlayerModel<CorpseEntity> slimModel;

    public CorpseRenderer(EntityRendererProvider.Context context) {
        this(context, new StaticPlayerModel(context, false), new StaticPlayerModel(context, true));
    }

    private CorpseRenderer(
            EntityRendererProvider.Context context,
            PlayerModel<CorpseEntity> wideModel,
            PlayerModel<CorpseEntity> slimModel
    ) {
        super(context, wideModel, 0.0F);
        this.wideModel = wideModel;
        this.slimModel = slimModel;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            CorpseEntity corpse,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        model = usesSlimSkin(corpse) ? slimModel : wideModel;
        model.setAllVisible(true);
        super.render(corpse, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity corpse) {
        return Minecraft.getInstance()
                .getSkinManager()
                .getInsecureSkinLocation(corpse.createOwnerProfile());
    }

    @Override
    protected void setupRotations(
            CorpseEntity corpse,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick
    ) {
        float groundYaw = DownedGeometry.groundYaw(corpse.downedPose());
        poseStack.translate(0.0F, GROUND_CLEARANCE, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - groundYaw));
        poseStack.translate(0.0F, 0.0F, -DownedGeometry.MODEL_CENTER_OFFSET);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    @Override
    protected void scale(CorpseEntity corpse, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected boolean shouldShowName(CorpseEntity corpse) {
        return false;
    }

    private static boolean usesSlimSkin(CorpseEntity corpse) {
        GameProfile profile = corpse.createOwnerProfile();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = Minecraft.getInstance()
                .getSkinManager()
                .getInsecureSkinInformation(profile);
        MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
        if (skin != null) {
            String modelName = skin.getMetadata("model");
            if (modelName != null) {
                return "slim".equals(modelName);
            }
        }
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(profile.getId()));
    }

    private static final class StaticPlayerModel extends PlayerModel<CorpseEntity> {
        private StaticPlayerModel(EntityRendererProvider.Context context, boolean slim) {
            super(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
        }

        @Override
        public void setupAnim(
                CorpseEntity corpse,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            super.setupAnim(corpse, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        }

        @Override
        public void renderToBuffer(
                PoseStack poseStack,
                VertexConsumer vertexConsumer,
                int packedLight,
                int packedOverlay,
                float red,
                float green,
                float blue,
                float alpha
        ) {
            setAllVisible(true);
            super.renderToBuffer(
                    poseStack,
                    vertexConsumer,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );
        }
    }
}
