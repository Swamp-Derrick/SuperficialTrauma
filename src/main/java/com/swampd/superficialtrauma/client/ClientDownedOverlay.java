package com.swampd.superficialtrauma.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public final class ClientDownedOverlay {
    private static final int MAX_BACKGROUND_ALPHA = 255;
    private static final float FADE_DELAY_PROGRESS = 0.15F;

    private ClientDownedOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible()) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        float progress = ClientDownedPoses.transitionProgress(minecraft.player.getId());
        float fadeProgress = Mth.clamp(
                (progress - FADE_DELAY_PROGRESS) / (1.0F - FADE_DELAY_PROGRESS),
                0.0F,
                1.0F
        );
        float easedProgress = smoothStep(fadeProgress);
        int backgroundAlpha = Mth.clamp(
                Math.round(MAX_BACKGROUND_ALPHA * easedProgress),
                0,
                MAX_BACKGROUND_ALPHA
        );
        graphics.fill(0, 0, width, height, backgroundAlpha << 24);

        float textProgress = Mth.clamp((progress - 0.65F) / 0.35F, 0.0F, 1.0F);
        int textAlpha = Mth.clamp(Math.round(255.0F * textProgress), 4, 255);
        int primaryColor = textAlpha << 24 | 0x00E8E8E8;
        int secondaryColor = textAlpha << 24 | 0x00A8A8A8;
        int warningColor = textAlpha << 24 | 0x00D8A84E;
        int centerY = height / 2;
        var state = ClientBodyState.snapshot();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();

        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("screen.superficialtrauma.downed.title"),
                width / 2,
                centerY - 28,
                primaryColor
        );
        graphics.drawCenteredString(
                minecraft.font,
                secondaryStatus(state),
                width / 2,
                centerY - 12,
                secondaryColor
        );
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable(
                        "screen.superficialtrauma.downed.total_countdown",
                        String.format(
                                Locale.ROOT,
                                "%.1f",
                                state.totalDownedDangerRemainingTicks(gameTime) / 20.0F
                        )
                ),
                width / 2,
                centerY + 4,
                secondaryColor
        );
        if (giveUpEligible(state.lifeState())) {
            Component giveUpText = ClientGiveUpState.isActive()
                    ? Component.translatable(
                            "screen.superficialtrauma.downed.give_up_progress",
                            String.format(Locale.ROOT, "%.1f", ClientGiveUpState.remainingSeconds())
                    )
                    : Component.translatable("screen.superficialtrauma.downed.give_up_hint");
            graphics.drawCenteredString(
                    minecraft.font,
                    giveUpText,
                    width / 2,
                    centerY + 20,
                    warningColor
            );
        }
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("screen.superficialtrauma.downed.health_hint"),
                width / 2,
                height - 28,
                secondaryColor
        );
        graphics.pose().popPose();
    }

    public static boolean isVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && ClientDownedPoses.get(minecraft.player.getId()).isPresent();
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static Component secondaryStatus(
            com.swampd.superficialtrauma.common.body.BodyState state
    ) {
        if (state.lifeState()
                == com.swampd.superficialtrauma.common.body.BodyLifeState.INCAPACITATED) {
            return Component.translatable(state.collapseReason().translationKey());
        }
        return Component.translatable(
                "life_state.superficialtrauma." + state.lifeState().serializedName()
        );
    }

    private static boolean giveUpEligible(
            com.swampd.superficialtrauma.common.body.BodyLifeState lifeState
    ) {
        return switch (lifeState) {
            case INCAPACITATED, CARDIAC_ARREST, VENTRICULAR_FIBRILLATION -> true;
            case ACTIVE, AWAKENING, BRAIN_DEAD -> false;
        };
    }
}
