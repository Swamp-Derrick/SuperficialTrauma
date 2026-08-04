package com.swampd.superficialtrauma.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ClientDownedOverlay {
    private static final int MAX_BACKGROUND_ALPHA = 224;

    private ClientDownedOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || ClientDownedPoses.get(minecraft.player.getId()).isEmpty()) {
            return;
        }

        float progress = ClientDownedPoses.transitionProgress(minecraft.player.getId());
        float easedProgress = smoothStep(progress);
        int backgroundAlpha = Mth.clamp(
                Math.round(MAX_BACKGROUND_ALPHA * easedProgress),
                0,
                MAX_BACKGROUND_ALPHA
        );
        graphics.fill(0, 0, width, height, backgroundAlpha << 24);

        float textProgress = Mth.clamp((progress - 0.45F) / 0.55F, 0.0F, 1.0F);
        int textAlpha = Mth.clamp(Math.round(255.0F * textProgress), 4, 255);
        int primaryColor = textAlpha << 24 | 0x00E8E8E8;
        int secondaryColor = textAlpha << 24 | 0x00A8A8A8;
        int centerY = height / 2;

        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("screen.superficialtrauma.downed.title"),
                width / 2,
                centerY - 12,
                primaryColor
        );
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable(
                        "life_state.superficialtrauma."
                                + ClientBodyState.snapshot().lifeState().serializedName()
                ),
                width / 2,
                centerY + 4,
                secondaryColor
        );
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("screen.superficialtrauma.downed.health_hint"),
                width / 2,
                height - 28,
                secondaryColor
        );
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
