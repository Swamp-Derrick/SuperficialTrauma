package com.swampd.superficialtrauma.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class ClientBloodLossOverlay {
    private static final int DISPLAY_TICKS = 36;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<BloodSpot> SPOTS = new ArrayList<>();
    private static int remainingTicks;

    private ClientBloodLossOverlay() {
    }

    public static void trigger(float amount) {
        SPOTS.clear();
        int count = amount >= 1.0F ? 3 : 2;
        for (int i = 0; i < count; i++) {
            SPOTS.add(createSpot());
        }
        remainingTicks = DISPLAY_TICKS;
    }

    public static void tick() {
        if (remainingTicks <= 0) {
            return;
        }
        remainingTicks--;
        if (remainingTicks == 0) {
            SPOTS.clear();
        }
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        if (remainingTicks <= 0 || SPOTS.isEmpty()) {
            return;
        }

        float life = Mth.clamp((remainingTicks - partialTick) / DISPLAY_TICKS, 0.0F, 1.0F);
        float fade = Mth.clamp(life * 1.35F, 0.0F, 1.0F);
        int shortSide = Math.min(screenWidth, screenHeight);
        for (BloodSpot spot : SPOTS) {
            int centerX = Math.round(spot.x * screenWidth);
            int centerY = Math.round(spot.y * screenHeight);
            int radius = Math.max(8, Math.round(spot.radius * shortSide));
            drawBlurredCircle(graphics, centerX, centerY, radius, spot.opacity * fade);
        }
    }

    public static void clear() {
        remainingTicks = 0;
        SPOTS.clear();
    }

    private static BloodSpot createSpot() {
        float x;
        float y;
        do {
            x = 0.08F + RANDOM.nextFloat() * 0.84F;
            y = 0.10F + RANDOM.nextFloat() * 0.80F;
        } while (Math.abs(x - 0.5F) < 0.16F && Math.abs(y - 0.5F) < 0.16F);

        return new BloodSpot(
                x,
                y,
                0.025F + RANDOM.nextFloat() * 0.020F,
                0.62F + RANDOM.nextFloat() * 0.18F
        );
    }

    private static void drawBlurredCircle(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int radius,
            float opacity
    ) {
        drawCircle(graphics, centerX, centerY, radius + 5, color(opacity * 0.08F));
        drawCircle(graphics, centerX, centerY, radius + 2, color(opacity * 0.13F));
        drawCircle(graphics, centerX, centerY, radius, color(opacity * 0.22F));
        drawCircle(graphics, centerX, centerY, Math.max(2, radius - 4), color(opacity * 0.18F));
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            int halfWidth = (int) Math.sqrt(radiusSquared - offsetY * offsetY);
            graphics.fill(
                    centerX - halfWidth,
                    centerY + offsetY,
                    centerX + halfWidth + 1,
                    centerY + offsetY + 1,
                    color
            );
        }
    }

    private static int color(float alpha) {
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return alphaByte << 24 | 0x790909;
    }

    private record BloodSpot(float x, float y, float radius, float opacity) {
    }
}
