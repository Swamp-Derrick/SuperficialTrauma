package com.swampd.superficialtrauma.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class HealthPanelTabButton extends AbstractButton {
    private static final int BORDER_COLOR = 0xFF68717F;
    private static final int SELECTED_BACKGROUND = 0xFF35453A;
    private static final int INACTIVE_BACKGROUND = 0xFF20252D;
    private static final int SELECTED_TEXT = 0xFFE8EDF2;
    private static final int INACTIVE_TEXT = 0xFF98A2AD;

    private final boolean selected;
    private final Runnable onPress;

    HealthPanelTabButton(
            int x,
            int y,
            int width,
            Component message,
            boolean selected,
            Runnable onPress
    ) {
        super(x, y, width, 18, message);
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = selected
                ? SELECTED_BACKGROUND
                : isHoveredOrFocused() ? 0xFF2C333D : INACTIVE_BACKGROUND;
        for (int row = 0; row < height; row++) {
            int inset = Math.max(0, 3 - row / 4);
            int rowY = getY() + row;
            graphics.fill(getX() + inset, rowY, getX() + width - inset, rowY + 1, background);
            graphics.fill(getX() + inset, rowY, getX() + inset + 1, rowY + 1, BORDER_COLOR);
            graphics.fill(getX() + width - inset - 1, rowY, getX() + width - inset, rowY + 1, BORDER_COLOR);
        }
        graphics.fill(getX() + 3, getY(), getX() + width - 3, getY() + 1, BORDER_COLOR);
        if (!selected) {
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, BORDER_COLOR);
        }

        int textColor = selected ? SELECTED_TEXT : INACTIVE_TEXT;
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        Component visibleMessage = Component.literal(font.plainSubstrByWidth(
                getMessage().getString(),
                Math.max(1, width - 8)
        ));
        graphics.drawCenteredString(
                font,
                visibleMessage,
                getX() + width / 2,
                getY() + 5,
                textColor
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
