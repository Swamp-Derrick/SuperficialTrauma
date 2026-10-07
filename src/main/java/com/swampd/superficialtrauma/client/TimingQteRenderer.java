package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

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
        if (qte == null || ClientTimingQteState.submitted()) {
            return;
        }
        float progress = Mth.clamp(ClientTimingQteState.presentProgress(), 0.0F, 1.0F);
        if (ClientTimingQteState.submitted()) return;

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
        // Exact arc boundaries: square dots used to protrude into the early-failure zone.
        graphics.flush();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = graphics.pose().last().pose();
        arc(vertices, pose, centerX, centerY, radius, 0, qte.perfectStart(), QTE_TRACK_COLOR);
        arc(vertices, pose, centerX, centerY, radius, qte.perfectStart(), qte.normalStart(), QTE_PERFECT_COLOR);
        arc(vertices, pose, centerX, centerY, radius, qte.normalStart(), qte.successEnd(), SUCCESS_COLOR);
        arc(vertices, pose, centerX, centerY, radius, qte.successEnd(), 1, QTE_TRACK_COLOR);
        graphics.flush();
    }

    private static void arc(VertexConsumer vertices, Matrix4f pose, int x, int y, int radius,
                            float start, float end, int color) {
        int segments = Math.max(1, (int) Math.ceil((end - start) * QTE_SEGMENTS));
        for (int i = 0; i < segments; i++) {
            double a = -Math.PI / 2 + (start + (end - start) * i / segments) * Math.PI * 2;
            double b = -Math.PI / 2 + (start + (end - start) * (i + 1) / segments) * Math.PI * 2;
            vertex(vertices, pose, x, y, radius - 2, a, color);
            vertex(vertices, pose, x, y, radius - 2, b, color);
            vertex(vertices, pose, x, y, radius + 3, b, color);
            vertex(vertices, pose, x, y, radius + 3, a, color);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, int x, int y,
                               int radius, double angle, int color) {
        vertices.addVertex(pose, x + (float) Math.cos(angle) * radius,
                y + (float) Math.sin(angle) * radius, 0).setColor(color);
    }

    private static void drawPanelBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.hLine(x, x + width - 1, y, BORDER_COLOR);
        graphics.hLine(x, x + width - 1, y + height - 1, BORDER_COLOR);
        graphics.vLine(x, y, y + height - 1, BORDER_COLOR);
        graphics.vLine(x + width - 1, y, y + height - 1, BORDER_COLOR);
    }

}
