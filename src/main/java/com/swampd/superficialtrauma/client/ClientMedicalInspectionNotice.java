package com.swampd.superficialtrauma.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ClientMedicalInspectionNotice {
    private static final long NOTICE_LIFETIME_MILLIS = 750L;

    private static String inspectorName = "";
    private static Component actionItemName;
    private static long expiresAtMillis;

    private ClientMedicalInspectionNotice() {
    }

    public static void update(String newInspectorName, Component newActionItemName) {
        if (newInspectorName == null || newInspectorName.isBlank()) {
            return;
        }
        boolean currentActionNotice = actionItemName != null && isActive();
        if (currentActionNotice && newActionItemName == null) {
            return;
        }
        inspectorName = newInspectorName;
        actionItemName = newActionItemName;
        expiresAtMillis = Util.getMillis() + NOTICE_LIFETIME_MILLIS;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !isActive()) {
            return;
        }

        Component inspection = Component.translatable(
                "screen.superficialtrauma.medical_notice.inspection",
                inspectorName
        );
        int firstY = height - 76;
        drawCenteredWithBackground(graphics, inspection, width, firstY);
        if (actionItemName != null) {
            Component action = Component.translatable(
                    "screen.superficialtrauma.medical_notice.action",
                    inspectorName,
                    actionItemName
            );
            drawCenteredWithBackground(graphics, action, width, firstY + 12);
        }
    }

    public static void clear() {
        inspectorName = "";
        actionItemName = null;
        expiresAtMillis = 0L;
    }

    private static boolean isActive() {
        if (expiresAtMillis <= Util.getMillis()) {
            clear();
            return false;
        }
        return true;
    }

    private static void drawCenteredWithBackground(
            GuiGraphics graphics,
            Component text,
            int width,
            int y
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int textWidth = minecraft.font.width(text);
        int x = (width - textWidth) / 2;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + minecraft.font.lineHeight + 2, 0xA0000000);
        graphics.drawString(minecraft.font, text, x, y, 0xFFE8E8E8, false);
    }
}
