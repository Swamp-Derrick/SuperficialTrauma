package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

final class DefibrillationEnergySlider extends AbstractSliderButton {
    private static final DefibrillationEnergy[] VALUES = DefibrillationEnergy.values();
    private static final int BORDER_COLOR = 0xFF68717F;
    private static final int SELECTED_COLOR = 0xFFE3B866;
    private final Consumer<DefibrillationEnergy> onChanged;
    private DefibrillationEnergy energy;

    DefibrillationEnergySlider(
            int x,
            int y,
            int width,
            DefibrillationEnergy initialEnergy,
            Consumer<DefibrillationEnergy> onChanged
    ) {
        super(
                x,
                y,
                width,
                18,
                Component.empty(),
                indexOf(initialEnergy) / (double) (VALUES.length - 1)
        );
        this.energy = initialEnergy == null ? DefibrillationEnergy.J150 : initialEnergy;
        this.onChanged = onChanged;
        updateMessage();
    }

    DefibrillationEnergy energy() {
        return energy;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(energy == null ? "150 J" : energy.joules() + " J"));
    }

    @Override
    protected void applyValue() {
        int index = Mth.clamp((int) Math.round(value * (VALUES.length - 1)), 0, VALUES.length - 1);
        value = index / (double) (VALUES.length - 1);
        DefibrillationEnergy next = VALUES[index];
        if (next != energy) {
            energy = next;
            onChanged.accept(next);
        }
        updateMessage();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int background = active ? 0xFF20252D : 0xFF292D33;
        graphics.fill(x, y, x + width, y + height, background);
        drawBorder(graphics, x, y, width, height, BORDER_COLOR);

        int segmentWidth = Math.max(1, (width - 4) / VALUES.length);
        Font font = Minecraft.getInstance().font;
        for (int index = 0; index < VALUES.length; index++) {
            int segmentStart = x + 2 + index * segmentWidth;
            int segmentEnd = index == VALUES.length - 1 ? x + width - 2 : segmentStart + segmentWidth;
            boolean selected = VALUES[index] == energy;
            if (selected) {
                graphics.fill(
                        segmentStart,
                        y + 2,
                        segmentEnd,
                        y + height - 2,
                        active ? 0xFF51462B : 0xFF33363B
                );
                graphics.fill(segmentStart, y + height - 3, segmentEnd, y + height - 1, SELECTED_COLOR);
            }
            if (index > 0) {
                graphics.fill(segmentStart, y + 3, segmentStart + 1, y + height - 3, 0xFF4A525D);
            }
            String label = Integer.toString(VALUES[index].joules());
            graphics.drawCenteredString(
                    font,
                    label,
                    segmentStart + (segmentEnd - segmentStart) / 2,
                    y + 5,
                    active ? (selected ? 0xFFFFD978 : 0xFFD4DAE1) : 0xFF777D85
            );
        }
    }

    private static int indexOf(DefibrillationEnergy energy) {
        DefibrillationEnergy candidate = energy == null ? DefibrillationEnergy.J150 : energy;
        for (int index = 0; index < VALUES.length; index++) {
            if (VALUES[index] == candidate) {
                return index;
            }
        }
        return 0;
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
