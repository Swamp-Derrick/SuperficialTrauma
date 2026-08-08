package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class MedicalActionButton extends AbstractButton {
    private final Item item;
    private final Runnable onPress;

    MedicalActionButton(
            int x,
            int y,
            Item item,
            Component message,
            Component tooltip,
            Runnable onPress
    ) {
        super(x, y, 22, 22, message);
        this.item = item;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (active) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int border = active
                ? (isHoveredOrFocused() ? 0xFFE3B866 : 0xFF68717F)
                : 0xFF68717F;
        int background = active ? 0xFF35453A : 0xFF292D33;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);

        if (!active) {
            RenderSystem.setShaderColor(0.45F, 0.45F, 0.45F, 1.0F);
        }
        graphics.renderItem(new ItemStack(item), x + 3, y + 3);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (!active) {
            graphics.fill(x + 2, y + 2, x + 20, y + 20, 0x66000000);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    Component tooltip() {
        return active
                ? getMessage()
                : Component.translatable("screen.superficialtrauma.health.item_not_held");
    }
}
