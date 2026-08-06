package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

public final class ClientAwakeningRecovery {
    private static final ResourceLocation BLUR_SHADER = ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID,
            "awakening_blur"
    );
    private static final float BLACK_FADE_PORTION = 0.35F;
    private static final float MAX_BLUR_RADIUS = 14.0F;

    private static long recoveryEndGameTime = -1L;
    private static ShaderInstance blurShader;
    private static TextureTarget sceneCopy;

    private ClientAwakeningRecovery() {
    }

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        BLUR_SHADER,
                        DefaultVertexFormat.POSITION_TEX
                ),
                shader -> blurShader = shader
        );
    }

    public static void synchronize(BodyState updated) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        if (!updated.isAwakeningRecoveryActive(gameTime)) {
            clear();
            return;
        }
        recoveryEndGameTime = updated.awakeningRecoveryEndGameTime();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || recoveryEndGameTime < 0L) {
            if (minecraft.level == null || minecraft.player == null) {
                clear();
            }
            return;
        }
        if (recoveryEndGameTime <= minecraft.level.getGameTime()) {
            clear();
        }
    }

    public static void renderWorldBlur(
            GuiGraphics graphics,
            int guiWidth,
            int guiHeight,
            float partialTick
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        float remainingTicks = remainingTicks(minecraft, partialTick);
        if (remainingTicks <= BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS
                || blurShader == null) {
            return;
        }

        float progress = visualProgress(remainingTicks);
        float clearProgress = smootherStep(Mth.clamp(
                (progress - BLACK_FADE_PORTION) / (1.0F - BLACK_FADE_PORTION),
                0.0F,
                1.0F
        ));
        float radius = MAX_BLUR_RADIUS * (1.0F - clearProgress);
        if (radius < 0.05F) {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        ensureSceneCopy(mainTarget.width, mainTarget.height);
        if (sceneCopy == null) {
            return;
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0,
                0,
                mainTarget.width,
                mainTarget.height,
                0,
                0,
                sceneCopy.width,
                sceneCopy.height,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        mainTarget.bindWrite(true);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> blurShader);
        RenderSystem.setShaderTexture(0, sceneCopy.getColorTextureId());
        blurShader.safeGetUniform("InSize").set(
                (float) sceneCopy.width,
                (float) sceneCopy.height
        );
        blurShader.safeGetUniform("Radius").set(radius);

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(pose, 0.0F, guiHeight, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(pose, guiWidth, guiHeight, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(pose, guiWidth, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(pose, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float remainingTicks = remainingTicks(minecraft, partialTick);
        if (remainingTicks <= BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS) {
            return;
        }

        float progress = visualProgress(remainingTicks);
        float blackFade = 1.0F - smootherStep(Mth.clamp(
                progress / BLACK_FADE_PORTION,
                0.0F,
                1.0F
        ));
        int blackAlpha = Mth.clamp(Math.round(255.0F * blackFade), 0, 255);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        if (blackAlpha > 0) {
            graphics.fill(0, 0, width, height, blackAlpha << 24);
        }
        graphics.pose().popPose();
    }

    public static void clear() {
        recoveryEndGameTime = -1L;
    }

    private static float remainingTicks(Minecraft minecraft, float partialTick) {
        if (minecraft.level == null || recoveryEndGameTime < 0L) {
            return -1.0F;
        }
        return recoveryEndGameTime
                - (minecraft.level.getGameTime() + Mth.clamp(partialTick, 0.0F, 1.0F));
    }

    private static void ensureSceneCopy(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (sceneCopy != null && sceneCopy.width == safeWidth && sceneCopy.height == safeHeight) {
            return;
        }
        if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
        }
        sceneCopy = new TextureTarget(safeWidth, safeHeight, false, Minecraft.ON_OSX);
        sceneCopy.setFilterMode(GL11.GL_LINEAR);
    }

    private static float visualProgress(float remainingTicks) {
        float visualRemaining = remainingTicks - BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS;
        return Mth.clamp(
                1.0F - visualRemaining / BodyState.AWAKENING_RECOVERY_VISUAL_DURATION_TICKS,
                0.0F,
                1.0F
        );
    }

    private static float smootherStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0F - 15.0F) + 10.0F);
    }
}
