package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public final class ClientAwakeningRecovery {
    private static final float FULL_BLACK_END_PORTION = 0.10F;
    private static final float EYES_FULLY_OPEN_PORTION = 0.82F;
    private static final float TEXT_FADE_IN_START_PORTION = 0.12F;
    private static final float TEXT_FULLY_VISIBLE_PORTION = 0.20F;
    private static final float TEXT_FADE_OUT_START_PORTION = 0.68F;
    private static final float TEXT_HIDDEN_PORTION = 0.90F;
    private static final int EYELID_EDGE_DEPTH = 8;

    private static long recoveryEndGameTime = -1L;
    private static List<String> contributorNames = List.of();

    private ClientAwakeningRecovery() {
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
        contributorNames = List.copyOf(updated.resuscitationContributorNames());
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

    public static void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float remainingTicks = remainingTicks(minecraft, partialTick);
        if (remainingTicks <= BodyState.AWAKENING_RECOVERY_SLOWDOWN_GRACE_TICKS) {
            return;
        }

        float progress = visualProgress(remainingTicks);
        float openingProgress = smootherStep(Mth.clamp(
                (progress - FULL_BLACK_END_PORTION)
                        / (EYES_FULLY_OPEN_PORTION - FULL_BLACK_END_PORTION),
                0.0F,
                1.0F
        ));
        int centerY = height / 2;
        int halfOpening = Math.round((height / 2.0F + 1.0F) * openingProgress);
        int upperEdge = Math.max(0, centerY - halfOpening);
        int lowerEdge = Math.min(height, centerY + halfOpening);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        if (upperEdge > 0) {
            graphics.fill(0, 0, width, upperEdge, 0xFF000000);
            drawUpperEyelidEdge(graphics, width, upperEdge, lowerEdge);
        }
        if (lowerEdge < height) {
            graphics.fill(0, lowerEdge, width, height, 0xFF000000);
            drawLowerEyelidEdge(graphics, width, upperEdge, lowerEdge);
        }
        renderRecoveryText(graphics, minecraft.font, width, height, progress);
        graphics.pose().popPose();
    }

    public static void clear() {
        recoveryEndGameTime = -1L;
        contributorNames = List.of();
    }

    private static void drawUpperEyelidEdge(
            GuiGraphics graphics,
            int width,
            int upperEdge,
            int lowerEdge
    ) {
        int available = Math.max(0, lowerEdge - upperEdge);
        int depth = Math.min(EYELID_EDGE_DEPTH, available / 2);
        for (int offset = 0; offset < depth; offset++) {
            int alpha = Math.round(130.0F * (1.0F - offset / (float) depth));
            graphics.fill(0, upperEdge + offset, width, upperEdge + offset + 1, alpha << 24);
        }
    }

    private static void drawLowerEyelidEdge(
            GuiGraphics graphics,
            int width,
            int upperEdge,
            int lowerEdge
    ) {
        int available = Math.max(0, lowerEdge - upperEdge);
        int depth = Math.min(EYELID_EDGE_DEPTH, available / 2);
        for (int offset = 0; offset < depth; offset++) {
            int alpha = Math.round(130.0F * (1.0F - offset / (float) depth));
            graphics.fill(0, lowerEdge - offset - 1, width, lowerEdge - offset, alpha << 24);
        }
    }

    private static void renderRecoveryText(
            GuiGraphics graphics,
            Font font,
            int width,
            int height,
            float progress
    ) {
        float fadeIn = smootherStep(Mth.clamp(
                (progress - TEXT_FADE_IN_START_PORTION)
                        / (TEXT_FULLY_VISIBLE_PORTION - TEXT_FADE_IN_START_PORTION),
                0.0F,
                1.0F
        ));
        float fadeOut = 1.0F - smootherStep(Mth.clamp(
                (progress - TEXT_FADE_OUT_START_PORTION)
                        / (TEXT_HIDDEN_PORTION - TEXT_FADE_OUT_START_PORTION),
                0.0F,
                1.0F
        ));
        int alpha = Mth.clamp(Math.round(255.0F * fadeIn * fadeOut), 0, 255);
        if (alpha <= 3) {
            return;
        }

        int color = alpha << 24 | 0xFFFFFF;
        Component title = Component.translatable("screen.superficialtrauma.awakening_recovered");
        int titleY = contributorNames.isEmpty() ? height / 2 - 4 : height / 2 - 12;
        graphics.drawCenteredString(font, title, width / 2, titleY, color);
        if (contributorNames.isEmpty()) {
            return;
        }

        Component rescuers = Component.translatable(
                "screen.superficialtrauma.awakening_rescuers",
                String.join("、", contributorNames)
        );
        List<FormattedCharSequence> lines = font.split(rescuers, Math.max(80, width - 40));
        int y = height / 2 + 3;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, (width - font.width(line)) / 2, y, color, false);
            y += font.lineHeight + 1;
        }
    }

    private static float remainingTicks(Minecraft minecraft, float partialTick) {
        if (minecraft.level == null || recoveryEndGameTime < 0L) {
            return -1.0F;
        }
        return recoveryEndGameTime
                - (minecraft.level.getGameTime() + Mth.clamp(partialTick, 0.0F, 1.0F));
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
