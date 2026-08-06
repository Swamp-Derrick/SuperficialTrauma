package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swampd.superficialtrauma.common.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

final class DefibrillationChargeButton extends AbstractButton {
    enum VisualState {
        IDLE,
        CHARGING,
        READY
    }

    private final Runnable onPress;
    private final Supplier<VisualState> visualState;
    private final DoubleSupplier progress;
    private final Supplier<Component> label;
    private final Supplier<Component> tooltip;

    DefibrillationChargeButton(
            int x,
            int y,
            int width,
            Runnable onPress,
            Supplier<VisualState> visualState,
            DoubleSupplier progress,
            Supplier<Component> label,
            Supplier<Component> tooltip
    ) {
        super(x, y, width, 24, Component.empty());
        this.onPress = onPress;
        this.visualState = visualState;
        this.progress = progress;
        this.label = label;
        this.tooltip = tooltip;
    }

    @Override
    public void onPress() {
        if (active && visualState.get() == VisualState.IDLE) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        VisualState state = visualState.get();
        int background;
        int border;
        if (!active && state == VisualState.IDLE) {
            background = 0xFF292D33;
            border = 0xFF68717F;
        } else if (state == VisualState.CHARGING) {
            background = 0xFF685627;
            border = 0xFFFFD15C;
        } else if (state == VisualState.READY) {
            background = 0xFF2F6542;
            border = 0xFF83E39A;
        } else {
            background = isHoveredOrFocused() ? 0xFF3D5544 : 0xFF35453A;
            border = isHoveredOrFocused() ? 0xFFE3B866 : 0xFF68717F;
        }

        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, background);
        drawBorder(graphics, x, y, width, height, border);
        if (state == VisualState.CHARGING) {
            int filled = Mth.clamp((int) Math.round((width - 4) * progress.getAsDouble()), 0, width - 4);
            graphics.fill(x + 2, y + height - 4, x + 2 + filled, y + height - 2, 0xFFFFD15C);
        } else if (state == VisualState.READY) {
            graphics.fill(x + 2, y + height - 4, x + width - 2, y + height - 2, 0xFF83E39A);
        }

        if (!active && state == VisualState.IDLE) {
            RenderSystem.setShaderColor(0.45F, 0.45F, 0.45F, 1.0F);
        }
        graphics.renderItem(new ItemStack(ModItems.DEFIBRILLATOR.get()), x + 4, y + 4);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Font font = Minecraft.getInstance().font;
        String visibleLabel = font.plainSubstrByWidth(label.get().getString(), Math.max(1, width - 28));
        graphics.drawCenteredString(
                font,
                visibleLabel,
                x + 17 + (width - 19) / 2,
                y + 8,
                !active && state == VisualState.IDLE ? 0xFF777D85 : 0xFFF0F3F6
        );
    }

    Component tooltip() {
        return tooltip.get();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
