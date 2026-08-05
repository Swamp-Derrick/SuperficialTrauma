package com.swampd.superficialtrauma.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ClientTreatmentOverlay {
    private ClientTreatmentOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientTreatmentState.isActive()
                || minecraft.player == null
                || minecraft.screen instanceof HealthScreen) {
            return;
        }

        ClientTreatmentState.ActiveTreatment active = ClientTreatmentState.activeTreatment();
        Component message = Component.translatable(
                "screen.superficialtrauma.health.treatment_progress",
                Component.translatable(active.type().translationKey()),
                String.format(java.util.Locale.ROOT, "%.1f", ClientTreatmentState.remainingSeconds())
        );
        int textWidth = minecraft.font.width(message);
        int x = (width - textWidth) / 2;
        int y = height - 62;
        graphics.fill(x - 5, y - 3, x + textWidth + 5, y + minecraft.font.lineHeight + 3, 0xB0000000);
        graphics.drawString(minecraft.font, message, x, y, 0xFFE3B866, false);
    }
}
