package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.loot.LootTargetMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class LootTargetScreen extends AbstractContainerScreen<LootTargetMenu> {
    private static final int PANEL_COLOR = 0xF0181D24;
    private static final int PANEL_BORDER_COLOR = 0xFF76808E;
    private static final int TARGET_SLOT_COLOR = 0xFF3B2D2B;
    private static final int PLAYER_SLOT_COLOR = 0xFF202A31;
    private static final int SLOT_BORDER_COLOR = 0xFF66717E;

    private Button takeAllButton;

    public LootTargetScreen(LootTargetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 204;
        imageHeight = 228;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelX = 8;
        inventoryLabelY = 130;
    }

    @Override
    protected void init() {
        super.init();
        takeAllButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.superficialtrauma.loot.take_all"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId,
                                        LootTargetMenu.TAKE_ALL_BUTTON_ID
                                );
                            }
                        }
                )
                .bounds(leftPos + 52, topPos + 106, 100, 18)
                .build());
        takeAllButton.active = menu.hasLootableItems();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (takeAllButton != null) {
            takeAllButton.active = menu.hasLootableItems();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, PANEL_COLOR);
        drawBorder(graphics, left, top, imageWidth, imageHeight, PANEL_BORDER_COLOR);

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            int slotLeft = left + slot.x - 1;
            int slotTop = top + slot.y - 1;
            int color = slotIndex < LootTargetMenu.TARGET_SLOT_COUNT
                    ? TARGET_SLOT_COLOR
                    : PLAYER_SLOT_COLOR;
            graphics.fill(slotLeft, slotTop, slotLeft + 18, slotTop + 18, color);
            drawBorder(graphics, slotLeft, slotTop, 18, 18, SLOT_BORDER_COLOR);
        }

        graphics.hLine(left + 7, left + imageWidth - 8, top + 126, PANEL_BORDER_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE7ECF2, false);
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0xC8D0DA,
                false
        );
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
