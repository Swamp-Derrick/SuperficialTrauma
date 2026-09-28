package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Identical timing ring and failure feedback for every medical action screen. */
public final class TimingQteRenderer {
    private static final int BORDER_COLOR = 0xFF76808E;
    private static final int TEXT_COLOR = 0xFFE7ECF2;
    private static final int SUCCESS_COLOR = 0xFF7DDC9A;
    private static final int QTE_TRACK_COLOR = 0xFF59626E;
    private static final int QTE_PERFECT_COLOR = 0xFFFFC94D;
    private static final int QTE_CURSOR_COLOR = 0xFFFFFFFF;
    private static final int QTE_SEGMENTS = 96;

    private TimingQteRenderer() { }

    public static void render(GuiGraphics graphics, Font font, int width, int height, float partialTick) {
        renderTimingQte(graphics, font, width, height, partialTick);
        renderQteFailureFlash(graphics, width, height);
    }

    private static void renderTimingQte(GuiGraphics graphics, Font font, int width, int height, float partialTick) {
        TimingQteSnapshot qte = ClientTimingQteState.active();
        if (qte == null) {
            return;
        }

        int radius = Math.min(58, Math.max(34, Math.min(width, height) / 7));
        int boxHalfWidth = radius + 34;
        int boxHalfHeight = radius + 30;
        int centerX = width / 2;
        int centerY = height / 2 + 4;
        graphics.fill(
                centerX - boxHalfWidth,
                centerY - boxHalfHeight,
                centerX + boxHalfWidth,
                centerY + boxHalfHeight,
                0xF0181D23
        );
        drawPanelBorder(
                graphics,
                centerX - boxHalfWidth,
                centerY - boxHalfHeight,
                boxHalfWidth * 2,
                boxHalfHeight * 2
        );

        drawQteRing(graphics, centerX, centerY, radius, qte);

        float progress = Mth.clamp(ClientTimingQteState.progress(partialTick), 0.0F, 1.0F);
        double cursorAngle = -Math.PI / 2.0D + progress * Math.PI * 2.0D;
        for (int step = -10; step <= 8; step += 2) {
            int cursorX = centerX + (int) Math.round(Math.cos(cursorAngle) * (radius + step));
            int cursorY = centerY + (int) Math.round(Math.sin(cursorAngle) * (radius + step));
            graphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, QTE_CURSOR_COLOR);
        }

        graphics.drawCenteredString(
                font,
                Component.translatable("screen.superficialtrauma.qte.space"),
                centerX,
                centerY - font.lineHeight / 2,
                TEXT_COLOR
        );
    }

    private static void renderQteFailureFlash(GuiGraphics graphics, int width, int height) {
        float strength = ClientTimingQteState.failureFlashStrength();
        if (strength <= 0.0F) {
            return;
        }
        int thickness = Math.max(10, Math.min(22, Math.min(width, height) / 24));
        for (int inset = 0; inset < thickness; inset++) {
            float inwardFade = 1.0F - inset / (float) thickness;
            int alpha = Mth.clamp((int) (72.0F * strength * inwardFade), 0, 72);
            int color = alpha << 24 | 0x00E02020;
            graphics.fill(0, inset, width, inset + 1, color);
            graphics.fill(0, height - inset - 1, width, height - inset, color);
            graphics.fill(inset, 0, inset + 1, height, color);
            graphics.fill(width - inset - 1, 0, width - inset, height, color);
        }
    }

    private static void drawQteRing(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int radius,
            TimingQteSnapshot qte
    ) {
        for (int index = 0; index < QTE_SEGMENTS; index++) {
            float fraction = index / (float) QTE_SEGMENTS;
            int color = QTE_TRACK_COLOR;
            if (fraction >= qte.perfectStart() && fraction < qte.normalStart()) {
                color = QTE_PERFECT_COLOR;
            } else if (fraction >= qte.normalStart() && fraction < qte.successEnd()) {
                color = SUCCESS_COLOR;
            }
            double angle = -Math.PI / 2.0D + fraction * Math.PI * 2.0D;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
        }
    }

    private static void drawPanelBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.hLine(x, x + width - 1, y, BORDER_COLOR);
        graphics.hLine(x, x + width - 1, y + height - 1, BORDER_COLOR);
        graphics.vLine(x, y, y + height - 1, BORDER_COLOR);
        graphics.vLine(x + width - 1, y, y + height - 1, BORDER_COLOR);
    }

}
