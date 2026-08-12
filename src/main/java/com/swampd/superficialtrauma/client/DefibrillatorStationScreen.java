package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.block.DefibrillatorStationBlockEntity;
import com.swampd.superficialtrauma.common.block.DefibrillatorStationMenu;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DefibrillatorStationScreen extends AbstractContainerScreen<DefibrillatorStationMenu> {
    private static final int PANEL_COLOR = 0xF0181D24;
    private static final int PANEL_BORDER_COLOR = 0xFF76808E;
    private static final int SLOT_COLOR = 0xFF27333B;
    private static final int SLOT_BORDER_COLOR = 0xFF66717E;
    private static final int BAR_BACKGROUND = 0xFF182126;
    private static final int BAR_CHARGE = 0xFF54D97B;
    private static final int BAR_WIDTH = 50;

    public DefibrillatorStationScreen(
            DefibrillatorStationMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 176;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 82;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_COLOR);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER_COLOR);

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            int slotLeft = leftPos + slot.x - 1;
            int slotTop = topPos + slot.y - 1;
            graphics.fill(slotLeft, slotTop, slotLeft + 18, slotTop + 18, SLOT_COLOR);
            drawBorder(graphics, slotLeft, slotTop, 18, 18, SLOT_BORDER_COLOR);
        }

        renderEnergyBar(graphics, 0, leftPos + 36, topPos + 55);
        renderEnergyBar(graphics, 1, leftPos + 90, topPos + 55);
        graphics.hLine(leftPos + 7, leftPos + imageWidth - 8, topPos + 78, PANEL_BORDER_COLOR);
    }

    private void renderEnergyBar(GuiGraphics graphics, int slotIndex, int x, int y) {
        ItemStack stack = menu.getSlot(slotIndex).getItem();
        int energy = stack.isEmpty() ? 0 : DefibrillatorItem.getEnergy(stack);
        int filled = Math.round(BAR_WIDTH * energy / (float) DefibrillatorItem.MAX_ENERGY);
        graphics.fill(x, y, x + BAR_WIDTH, y + 6, BAR_BACKGROUND);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 6, BAR_CHARGE);
        }
        drawBorder(graphics, x, y, BAR_WIDTH, 6, SLOT_BORDER_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE7ECF2, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC8D0DA, false);

        for (int slotIndex = 0; slotIndex < DefibrillatorStationBlockEntity.SLOT_COUNT; slotIndex++) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            Component energyText = stack.isEmpty()
                    ? Component.translatable("screen.superficialtrauma.defibrillator_station.empty")
                    : Component.translatable(
                            "screen.superficialtrauma.defibrillator_station.energy",
                            DefibrillatorItem.getEnergy(stack),
                            DefibrillatorItem.MAX_ENERGY
                    );
            int centerX = slotIndex == 0 ? 61 : 115;
            graphics.drawCenteredString(font, energyText, centerX, 20, 0xC8D0DA);
        }
    }

    private static void drawBorder(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.hLine(x, x + width - 1, y, color);
        graphics.hLine(x, x + width - 1, y + height - 1, color);
        graphics.vLine(x, y, y + height - 1, color);
        graphics.vLine(x + width - 1, y, y + height - 1, color);
    }
}
