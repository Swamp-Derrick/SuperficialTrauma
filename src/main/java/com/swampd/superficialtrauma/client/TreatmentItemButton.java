package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class TreatmentItemButton extends AbstractButton {
    private final TreatmentType treatmentType;
    private final boolean removal;
    private final boolean missingRequiredItem;
    private final Runnable onPress;
    private final int clipLeft;
    private final int clipTop;
    private final int clipRight;
    private final int clipBottom;

    TreatmentItemButton(
            int x,
            int y,
            TreatmentType treatmentType,
            boolean removal,
            Component message,
            boolean missingRequiredItem,
            Runnable onPress,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom
    ) {
        super(x, y, 22, 22, message);
        this.treatmentType = treatmentType;
        this.removal = removal;
        this.missingRequiredItem = missingRequiredItem;
        this.onPress = onPress;
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    @Override
    public void onPress() {
        if (active) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (getX() >= clipRight
                || getX() + width <= clipLeft
                || getY() >= clipBottom
                || getY() + height <= clipTop) {
            return;
        }
        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        int x = getX();
        int y = getY();
        int border = !active
                ? 0xFF68717F
                : removal
                        ? (isHoveredOrFocused() ? 0xFFFF9B9B : 0xFFE06C75)
                        : (isHoveredOrFocused() ? 0xFFE3B866 : 0xFF68717F);
        int background = !active
                ? 0xFF292D33
                : removal ? 0xFF5A2529 : 0xFF35453A;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);

        ItemStack icon = new ItemStack(treatmentType.requiredItem());
        if (!active) {
            RenderSystem.setShaderColor(0.45F, 0.45F, 0.45F, 1.0F);
        }
        graphics.renderItem(icon, x + 3, y + 3);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (!active) {
            graphics.fill(x + 2, y + 2, x + 20, y + 20, 0x66000000);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= clipLeft
                && mouseX < clipRight
                && mouseY >= clipTop
                && mouseY < clipBottom
                && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    Component tooltip() {
        return treatmentType.requiredItem().getDescription();
    }
}
