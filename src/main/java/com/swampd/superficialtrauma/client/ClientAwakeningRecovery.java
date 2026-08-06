package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.List;

public final class ClientAwakeningRecovery {
    private static final ResourceLocation BLUR_EFFECT = ResourceLocation.fromNamespaceAndPath(
            SuperficialTrauma.MOD_ID,
            "shaders/post/awakening_blur.json"
    );
    private static final float BLACK_FADE_PORTION = 0.35F;
    private static final float MAX_BLUR_RADIUS = 18.0F;
    private static final int MAX_HAZE_ALPHA = 34;

    private static long recoveryEndGameTime = -1L;
    private static PostChain ownedBlurEffect;
    private static List<PostPass> blurPasses = List.of();
    private static boolean warnedAboutShaderAccess;

    private ClientAwakeningRecovery() {
    }

    public static void synchronize(BodyState updated) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        long newEndGameTime = updated.awakeningRecoveryEndGameTime();
        if (!updated.isAwakeningRecoveryActive(gameTime)) {
            clear();
            return;
        }
        if (recoveryEndGameTime != newEndGameTime) {
            releaseBlurEffect();
            recoveryEndGameTime = newEndGameTime;
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || recoveryEndGameTime < 0L) {
            if (minecraft.level == null || minecraft.player == null) {
                clear();
            }
            return;
        }

        float remainingTicks = recoveryEndGameTime - minecraft.level.getGameTime();
        if (remainingTicks <= 0.0F) {
            clear();
            return;
        }
        if (remainingTicks <= BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS) {
            releaseBlurEffect();
            return;
        }

        ensureBlurEffect();
        updateBlurRadius(visualProgress(remainingTicks));
    }

    public static void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || recoveryEndGameTime < 0L) {
            return;
        }
        float remainingTicks = recoveryEndGameTime
                - (minecraft.level.getGameTime() + Mth.clamp(partialTick, 0.0F, 1.0F));
        if (remainingTicks <= BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS) {
            return;
        }

        float progress = visualProgress(remainingTicks);
        float blurFade = 1.0F - smootherStep(Mth.clamp(
                (progress - BLACK_FADE_PORTION) / (1.0F - BLACK_FADE_PORTION),
                0.0F,
                1.0F
        ));
        int hazeAlpha = Mth.clamp(Math.round(MAX_HAZE_ALPHA * blurFade), 0, MAX_HAZE_ALPHA);
        float blackFade = 1.0F - smootherStep(Mth.clamp(
                progress / BLACK_FADE_PORTION,
                0.0F,
                1.0F
        ));
        int blackAlpha = Mth.clamp(Math.round(255.0F * blackFade), 0, 255);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        if (hazeAlpha > 0) {
            graphics.fill(0, 0, width, height, hazeAlpha << 24 | 0x00C4CBD1);
        }
        if (blackAlpha > 0) {
            graphics.fill(0, 0, width, height, blackAlpha << 24);
        }
        graphics.pose().popPose();
    }

    public static void clear() {
        recoveryEndGameTime = -1L;
        releaseBlurEffect();
    }

    private static float visualProgress(float remainingTicks) {
        float visualRemaining = remainingTicks - BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS;
        return Mth.clamp(
                1.0F - visualRemaining / BodyState.AWAKENING_RECOVERY_VISUAL_DURATION_TICKS,
                0.0F,
                1.0F
        );
    }

    private static void ensureBlurEffect() {
        Minecraft minecraft = Minecraft.getInstance();
        PostChain current = minecraft.gameRenderer.currentEffect();
        if (ownedBlurEffect != null) {
            if (current != ownedBlurEffect) {
                ownedBlurEffect = null;
                blurPasses = List.of();
            }
            return;
        }
        if (current != null) {
            return;
        }

        minecraft.gameRenderer.loadEffect(BLUR_EFFECT);
        PostChain loaded = minecraft.gameRenderer.currentEffect();
        if (loaded == null || !BLUR_EFFECT.toString().equals(loaded.getName())) {
            return;
        }
        ownedBlurEffect = loaded;
        try {
            List<PostPass> passes = ObfuscationReflectionHelper.getPrivateValue(
                    PostChain.class,
                    loaded,
                    "f_110009_"
            );
            blurPasses = passes == null ? List.of() : List.copyOf(passes);
        } catch (RuntimeException exception) {
            blurPasses = List.of();
            if (!warnedAboutShaderAccess) {
                warnedAboutShaderAccess = true;
                SuperficialTrauma.LOGGER.warn(
                        "Could not adjust awakening blur radius; the black-to-clear overlay will still render",
                        exception
                );
            }
        }
    }

    private static void updateBlurRadius(float progress) {
        if (blurPasses.isEmpty()) {
            return;
        }
        float clearProgress = smootherStep(Mth.clamp(
                (progress - BLACK_FADE_PORTION) / (1.0F - BLACK_FADE_PORTION),
                0.0F,
                1.0F
        ));
        float radius = Math.max(1.0F, MAX_BLUR_RADIUS * (1.0F - clearProgress));
        for (PostPass pass : blurPasses) {
            pass.getEffect().safeGetUniform("Radius").set(radius);
        }
    }

    private static void releaseBlurEffect() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ownedBlurEffect != null && minecraft.gameRenderer.currentEffect() == ownedBlurEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        ownedBlurEffect = null;
        blurPasses = List.of();
    }

    private static float smootherStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0F - 15.0F) + 10.0F);
    }
}
