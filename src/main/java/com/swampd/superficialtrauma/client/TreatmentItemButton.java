package com.swampd.superficialtrauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

final class TreatmentItemButton extends AbstractButton {
    private final UUID woundId;
    private final TreatmentType treatmentType;
    private final TreatmentAction action;
    private final Runnable onPress;

    TreatmentItemButton(
            int x,
            int y,
            UUID woundId,
            TreatmentType treatmentType,
            TreatmentAction action,
            Runnable onPress
    ) {
        super(x, y, 22, 22, Component.translatable(action.translationKey(treatmentType)));
        this.woundId = woundId;
        this.treatmentType = treatmentType;
        this.action = action;
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
        boolean removal = action == TreatmentAction.REMOVE;
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
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    UUID woundId() {
        return woundId;
    }

    TreatmentType treatmentType() {
        return treatmentType;
    }

    TreatmentAction action() {
        return action;
    }

    Component tooltip() {
        if (action == TreatmentAction.REMOVE) {
            return Component.translatable(
                    "screen.superficialtrauma.health.treatment_remove_tooltip",
                    Component.translatable(action.translationKey(treatmentType)),
                    treatmentType.durationTicks() / 20L
            );
        }
        return Component.translatable(
                "screen.superficialtrauma.health.treatment_tooltip",
                Component.translatable(treatmentType.translationKey()),
                treatmentType.requiredCount(),
                treatmentType.requiredItem().getDescription()
        );
    }
}
